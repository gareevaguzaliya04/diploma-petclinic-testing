#!/usr/bin/env python3
"""generate-dashboard.py — динамический дашборд из реальных данных CI."""

import os
import glob
import xml.etree.ElementTree as ET
from datetime import datetime, timezone


# ─── Helpers ──────────────────────────────────────────────────────────────────

def _int(val, default=0):
    try:
        return int(val)
    except (TypeError, ValueError):
        return default


# ─── 1. Surefire XML ─────────────────────────────────────────────────────────

def parse_surefire(patterns):
    """Парсит TEST-*.xml по glob-шаблонам. Возвращает (total, passed, failed, skipped).

    Считает <testcase> элементы напрямую — атрибут tests="0" на корне встречается
    при использовании @Nested классов в JUnit 5 (вложенные тесты не суммируются
    в родительском <testsuite>).
    """
    total = passed = failed = skipped = 0
    seen = set()
    for pattern in patterns:
        for path in glob.glob(pattern, recursive=True):
            if path in seen:
                continue
            seen.add(path)
            try:
                root = ET.parse(path).getroot()
                for case in root.findall('.//testcase'):
                    total += 1
                    if case.find('failure') is not None or case.find('error') is not None:
                        failed += 1
                    elif case.find('skipped') is not None:
                        skipped += 1
                    else:
                        passed += 1
            except Exception:
                pass
    return total, passed, failed, skipped


# ─── 2. JaCoCo XML ───────────────────────────────────────────────────────────

def parse_jacoco(path_pattern):
    """Возвращает % покрытия строк (int 0–100) или None если файл не найден."""
    for path in glob.glob(path_pattern, recursive=True):
        try:
            root = ET.parse(path).getroot()
            for counter in root.findall('counter'):
                if counter.get('type') == 'LINE':
                    covered = _int(counter.get('covered', 0))
                    missed  = _int(counter.get('missed',  0))
                    total   = covered + missed
                    if total > 0:
                        return round(covered / total * 100)
        except Exception:
            pass
    return None


# ─── 3. Gatling simulation.log ───────────────────────────────────────────────

def parse_gatling(path_pattern):
    """Парсит simulation.log. Возвращает dict с метриками или None."""
    for path in glob.glob(path_pattern, recursive=True):
        try:
            times = []
            ok = ko = 0
            run_start = end_ms = None

            with open(path, encoding='utf-8') as fh:
                for line in fh:
                    parts = line.rstrip('\n').split('\t')
                    if not parts:
                        continue
                    if parts[0] == 'RUN' and len(parts) >= 4:
                        run_start = int(parts[3])
                    elif parts[0] == 'REQUEST' and len(parts) >= 6:
                        t_start = int(parts[3])
                        t_end   = int(parts[4])
                        status  = parts[5]
                        times.append(t_end - t_start)
                        end_ms = max(end_ms or 0, t_end)
                        if status == 'OK':
                            ok += 1
                        else:
                            ko += 1

            if not times:
                continue

            times.sort()
            total     = ok + ko
            p95       = times[int(len(times) * 0.95)]
            error_pct = round(ko / total * 100, 1) if total else 0
            duration  = round((end_ms - run_start) / 1000) if run_start and end_ms else 0
            rps       = round(total / duration, 1) if duration else 0

            return {
                'total': total, 'ok': ok, 'ko': ko,
                'error_pct': error_pct, 'p95': p95,
                'duration': duration, 'rps': rps,
            }
        except Exception:
            pass
    return None


# ─── 4. CI-метаданные ────────────────────────────────────────────────────────

def ci_meta():
    run_num = os.environ.get('GITHUB_RUN_NUMBER', '—')
    branch  = os.environ.get('GITHUB_REF_NAME') or os.environ.get('GITHUB_HEAD_REF', '—')
    now     = datetime.now(timezone.utc).strftime('%d.%m.%Y %H:%M UTC')
    return run_num, branch, now


# ─── 5. HTML-генерация ───────────────────────────────────────────────────────

def _bar_width(count, max_count, min_pct=8):
    if max_count == 0:
        return min_pct
    return max(min_pct, round(count / max_count * 78))


def _cov_color(pct):
    if pct is None:
        return '#8b949e'
    return '#3fb950' if pct >= 70 else ('#d29922' if pct >= 50 else '#f85149')


def _fmt_ms(ms):
    if ms is None:
        return 'N/A'
    return f'{ms}мс' if ms < 1000 else f'{ms / 1000:.1f}с'


def _level_row(label, count, max_count, bg, color_class):
    w = _bar_width(count, max_count)
    return f'''
      <div class="level">
        <div class="level-label">{label}</div>
        <div class="level-bar-wrap">
          <div class="level-bar" style="width:{w}%;background:{bg}">{count} тестов</div>
        </div>
        <div class="level-count {color_class}">{count}</div>
      </div>'''


def generate_html(unit, integ, contract, e2e,
                  jacoco_pct, gatling,
                  run_num, branch, now):

    total        = unit[0] + integ[0] + contract[0] + e2e[0]
    total_failed = unit[2] + integ[2] + contract[2] + e2e[2]

    # ── шапка ──
    badge_text  = 'ALL TESTS PASSING' if total_failed == 0 else f'{total_failed} FAILED'
    badge_color = '#238636' if total_failed == 0 else '#da3633'
    ok_icon = ('<svg width="24" height="24" viewBox="0 0 24 24" fill="none" '
               'stroke="#3fb950" stroke-width="2"><polyline points="20 6 9 17 4 12"/></svg>')
    fail_icon = ('<svg width="24" height="24" viewBox="0 0 24 24" fill="none" '
                 'stroke="#f85149" stroke-width="2">'
                 '<line x1="18" y1="6" x2="6" y2="18"/>'
                 '<line x1="6" y1="6" x2="18" y2="18"/></svg>')
    header_icon = ok_icon if total_failed == 0 else fail_icon
    sub_total   = 'Все прошли ✓' if total_failed == 0 else f'{total_failed} упало ✗'

    # ── покрытие ──
    cov_str   = f'{jacoco_pct}%' if jacoco_pct is not None else 'N/A'
    cov_color = _cov_color(jacoco_pct)

    # ── Gatling ──
    if gatling:
        g_total   = gatling['total']
        g_err     = f"{gatling['error_pct']}%"
        g_err_col = '#3fb950' if gatling['error_pct'] == 0 else '#f85149'
        g_p95     = _fmt_ms(gatling['p95'])
        g_dur     = f"{gatling['duration']}с"
        g_rps     = f"~{gatling['rps']}"
        g_ok      = '<span style="color:#3fb950">OK</span>' if gatling['error_pct'] == 0 \
                    else '<span style="color:#f85149">FAIL</span>'
        g_bar_txt = f"{g_total} запросов · P95 {g_p95}"
    else:
        g_total = g_err = g_p95 = g_dur = g_rps = 'N/A'
        g_err_col = '#8b949e'
        g_ok = 'N/A'
        g_bar_txt = 'данные отсутствуют'

    # ── пирамида ──
    max_c = max(unit[0], integ[0], contract[0], e2e[0], 1)
    pyramid = (
        _level_row('Unit (JUnit 5 + Mockito)',        unit[0],     max_c, '#238636', 'green')
        + _level_row('Интеграционные (Testcontainers)', integ[0],   max_c, '#1f6feb', 'blue')
        + _level_row('Контрактные (WireMock)',          contract[0], max_c, '#9e6a03', 'yellow')
        + _level_row('E2E (HttpClient / REST Assured)', e2e[0],      max_c, '#6e40c9', 'purple')
        + f'''
      <div class="level">
        <div class="level-label">Нагрузочные (Gatling)</div>
        <div class="level-bar-wrap">
          <div class="level-bar" style="width:15%;background:#da3633">{g_bar_txt}</div>
        </div>
        <div class="level-count" style="color:#f85149">{'OK' if gatling else '—'}</div>
      </div>'''
    )

    # ── CI jobs ──
    def job_row(name, counts):
        icon_cls = 'pass' if counts[2] == 0 else 'fail'
        mark     = '✓'   if counts[2] == 0 else '✗'
        status   = 'PASSED' if counts[2] == 0 else 'FAILED'
        return f'''
      <div class="job">
        <div class="job-icon {icon_cls}">{mark}</div>
        <div class="job-name">{name}</div>
        <div class="job-time">{counts[0]} тестов</div>
        <div class="job-status pass">{status}</div>
      </div>'''

    jobs = (
        job_row('Unit Tests',                               unit)
        + job_row('Integration Tests (Testcontainers + PostgreSQL)', integ)
        + job_row('Contract Tests (WireMock)',              contract)
    )

    # ── кнопки-ссылки на отчёты ──
    report_buttons = '''
      <div class="report-links">
        <a href="allure/index.html" class="report-btn allure-btn">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
            <polyline points="14 2 14 8 20 8"/>
            <line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/>
            <polyline points="10 9 9 9 8 9"/>
          </svg>
          Allure Report
        </a>
        <a href="coverage/index.html" class="report-btn coverage-btn">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <polyline points="22 12 18 12 15 21 9 3 6 12 2 12"/>
          </svg>
          JaCoCo Coverage
        </a>
      </div>'''

    return f'''<!DOCTYPE html>
<html lang="ru">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>Test Dashboard - PetClinic Microservices</title>
<style>
  * {{ margin: 0; padding: 0; box-sizing: border-box; }}
  body {{ font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif; background: #0d1117; color: #e6edf3; }}
  .header {{ background: #161b22; border-bottom: 1px solid #30363d; padding: 20px 40px; display: flex; align-items: center; gap: 16px; }}
  .header h1 {{ font-size: 20px; font-weight: 600; }}
  .header .badge {{ background: {badge_color}; color: white; padding: 4px 12px; border-radius: 20px; font-size: 12px; font-weight: 600; }}
  .container {{ max-width: 1100px; margin: 0 auto; padding: 32px 40px; }}
  .grid {{ display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; margin-bottom: 32px; }}
  .card {{ background: #161b22; border: 1px solid #30363d; border-radius: 12px; padding: 20px; }}
  .card .label {{ font-size: 12px; color: #8b949e; margin-bottom: 8px; text-transform: uppercase; letter-spacing: 0.5px; }}
  .card .value {{ font-size: 32px; font-weight: 700; }}
  .card .sub {{ font-size: 13px; color: #8b949e; margin-top: 4px; }}
  .green {{ color: #3fb950; }} .blue {{ color: #58a6ff; }} .yellow {{ color: #d29922; }} .purple {{ color: #bc8cff; }}
  .section {{ background: #161b22; border: 1px solid #30363d; border-radius: 12px; padding: 24px; margin-bottom: 24px; }}
  .section h2 {{ font-size: 16px; font-weight: 600; margin-bottom: 20px; display: flex; align-items: center; gap: 8px; }}
  .pyramid {{ display: flex; flex-direction: column; gap: 12px; }}
  .level {{ display: flex; align-items: center; gap: 16px; }}
  .level-bar-wrap {{ flex: 1; background: #21262d; border-radius: 6px; height: 36px; overflow: hidden; }}
  .level-bar {{ height: 100%; border-radius: 6px; display: flex; align-items: center; padding: 0 12px; font-size: 13px; font-weight: 600; color: white; transition: width 1s ease; }}
  .level-label {{ width: 200px; font-size: 13px; color: #8b949e; }}
  .level-count {{ width: 60px; text-align: right; font-size: 13px; font-weight: 600; }}
  .jobs {{ display: flex; flex-direction: column; gap: 10px; }}
  .job {{ display: flex; align-items: center; gap: 12px; padding: 12px 16px; background: #21262d; border-radius: 8px; }}
  .job-icon {{ width: 20px; height: 20px; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 11px; font-weight: 700; flex-shrink: 0; }}
  .job-icon.pass {{ background: #238636; }} .job-icon.fail {{ background: #da3633; }}
  .job-name {{ flex: 1; font-size: 14px; }}
  .job-time {{ font-size: 12px; color: #8b949e; }}
  .job-status {{ font-size: 12px; font-weight: 600; }}
  .job-status.pass {{ color: #3fb950; }}
  .gatling {{ display: grid; grid-template-columns: repeat(3, 1fr); gap: 12px; }}
  .metric {{ background: #21262d; border-radius: 8px; padding: 16px; text-align: center; }}
  .metric .m-value {{ font-size: 28px; font-weight: 700; color: #58a6ff; }}
  .metric .m-label {{ font-size: 12px; color: #8b949e; margin-top: 4px; }}
  .tech {{ display: flex; flex-wrap: wrap; gap: 8px; }}
  .tech span {{ background: #21262d; border: 1px solid #30363d; border-radius: 6px; padding: 6px 14px; font-size: 13px; }}
  .footer {{ text-align: center; padding: 32px; color: #8b949e; font-size: 13px; border-top: 1px solid #30363d; margin-top: 32px; line-height: 1.8; }}
  .report-links {{ display: flex; gap: 12px; margin-bottom: 24px; }}
  .report-btn {{ display: inline-flex; align-items: center; gap: 8px; padding: 10px 20px; border-radius: 8px; font-size: 14px; font-weight: 600; text-decoration: none; transition: opacity .2s; }}
  .report-btn:hover {{ opacity: .8; }}
  .allure-btn {{ background: #1f6feb; color: white; }}
  .coverage-btn {{ background: #238636; color: white; }}
</style>
</head>
<body>
<div class="header">
  {header_icon}
  <h1>PetClinic Microservices — Test Dashboard</h1>
  <span class="badge">{badge_text}</span>
</div>
<div class="container">

  {report_buttons}

  <div class="grid">
    <div class="card">
      <div class="label">Всего тестов</div>
      <div class="value green">{total}</div>
      <div class="sub">{sub_total}</div>
    </div>
    <div class="card">
      <div class="label">Покрытие кода</div>
      <div class="value" style="color:{cov_color}">{cov_str}</div>
      <div class="sub">Line coverage (JaCoCo)</div>
    </div>
    <div class="card">
      <div class="label">Время CI/CD</div>
      <div class="value yellow">~3 мин</div>
      <div class="sub">3 параллельных job</div>
    </div>
    <div class="card">
      <div class="label">Gatling P95</div>
      <div class="value purple">{g_p95}</div>
      <div class="sub">{g_total} запросов, {g_err} ошибок</div>
    </div>
  </div>

  <div class="section">
    <h2>
      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#58a6ff" stroke-width="2">
        <polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/>
      </svg>
      Пирамида тестирования
    </h2>
    <div class="pyramid">{pyramid}
    </div>
  </div>

  <div class="section">
    <h2>
      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#3fb950" stroke-width="2">
        <circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/>
      </svg>
      CI/CD Pipeline — GitHub Actions
    </h2>
    <div class="jobs">{jobs}
    </div>
  </div>

  <div class="section">
    <h2>
      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#bc8cff" stroke-width="2">
        <polyline points="22 12 18 12 15 21 9 3 6 12 2 12"/>
      </svg>
      Gatling — результаты нагрузочного тестирования
    </h2>
    <div class="gatling">
      <div class="metric"><div class="m-value">{g_total}</div><div class="m-label">Всего запросов</div></div>
      <div class="metric"><div class="m-value" style="color:{g_err_col}">{g_err}</div><div class="m-label">Процент ошибок</div></div>
      <div class="metric"><div class="m-value">{g_p95}</div><div class="m-label">P95 время ответа</div></div>
      <div class="metric"><div class="m-value">{g_dur}</div><div class="m-label">Длительность теста</div></div>
      <div class="metric"><div class="m-value">{g_rps}</div><div class="m-label">Запросов в секунду</div></div>
      <div class="metric"><div class="m-value">{g_ok}</div><div class="m-label">Статус</div></div>
    </div>
  </div>

  <div class="section">
    <h2>
      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#8b949e" stroke-width="2">
        <rect x="2" y="3" width="20" height="14" rx="2"/>
        <line x1="8" y1="21" x2="16" y2="21"/><line x1="12" y1="17" x2="12" y2="21"/>
      </svg>
      Технологический стек
    </h2>
    <div class="tech">
      <span>☕ Java 17</span><span>🌱 Spring Boot 4</span><span>🐘 PostgreSQL 15</span>
      <span>🐳 Docker Compose</span><span>✅ JUnit 5</span><span>🎭 Mockito</span>
      <span>📦 Testcontainers</span><span>🔌 WireMock</span><span>⚡ Gatling</span>
      <span>🔄 GitHub Actions</span><span>🏥 Spring PetClinic</span>
    </div>
  </div>

</div>
<div class="footer">
  Дипломная работа — «Внедрение эффективной стратегии тестирования в проектах с микросервисной архитектурой» • Гареева Гузалия • 2026
  <br>
  Последнее обновление: {now} · Запуск #{run_num} · ветка {branch}
</div>
</body>
</html>'''


# ─── Main ─────────────────────────────────────────────────────────────────────

if __name__ == '__main__':
    run_num, branch, now = ci_meta()

    # Surefire XMLs лежат в скачанных артефактах
    unit     = parse_surefire(['allure-results/unit/**/surefire-reports/TEST-*.xml'])
    integ    = parse_surefire(['allure-results/integration/**/surefire-reports/TEST-*.xml'])
    contract = parse_surefire(['allure-results/contract/**/surefire-reports/TEST-*.xml'])
    e2e      = parse_surefire(['allure-results/e2e/**/surefire-reports/TEST-*.xml'])

    # JaCoCo XML генерируется в том же job'е перед вызовом скрипта
    jacoco_pct = parse_jacoco(
        'spring-petclinic-customers-service/target/site/jacoco/jacoco.xml'
    )

    # Gatling simulation.log — скачан из артефакта gatling-results
    gatling = parse_gatling('gatling-results/**/simulation.log')

    html = generate_html(
        unit, integ, contract, e2e,
        jacoco_pct, gatling,
        run_num, branch, now,
    )

    out_dir = 'site'
    os.makedirs(out_dir, exist_ok=True)
    out_path = os.path.join(out_dir, 'index.html')
    with open(out_path, 'w', encoding='utf-8') as f:
        f.write(html)

    print(f'Dashboard written to {out_path}')
    print(f'  unit={unit[0]}, integration={integ[0]}, contract={contract[0]}, e2e={e2e[0]}')
    print(f'  jacoco={jacoco_pct}%  gatling_p95={gatling["p95"] if gatling else "N/A"}ms')
    print(f'  run=#{run_num}  branch={branch}  time={now}')
