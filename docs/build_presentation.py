"""
AI News Curator - 발표 자료 생성기 (PPTX + PDF 동시 생성)
"""
from pptx import Presentation
from pptx.util import Inches, Pt, Emu
from pptx.dml.color import RGBColor
from pptx.enum.shapes import MSO_SHAPE
from pptx.enum.text import PP_ALIGN, MSO_ANCHOR

from reportlab.lib.pagesizes import landscape
from reportlab.pdfgen import canvas
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.lib.colors import HexColor

import os

# ─── 색상 팔레트 ──────────────────────────────────────────
NAVY      = RGBColor(0x0F, 0x1B, 0x3D)
ACCENT    = RGBColor(0x4F, 0x8C, 0xFF)
ACCENT2   = RGBColor(0x00, 0xC2, 0xA8)
LIGHT_BG  = RGBColor(0xF5, 0xF7, 0xFB)
DARK_TEXT = RGBColor(0x1A, 0x20, 0x2C)
GRAY_TEXT = RGBColor(0x55, 0x5F, 0x70)
WHITE     = RGBColor(0xFF, 0xFF, 0xFF)

PDF_NAVY    = HexColor("#0F1B3D")
PDF_ACCENT  = HexColor("#4F8CFF")
PDF_ACCENT2 = HexColor("#00C2A8")
PDF_LIGHT   = HexColor("#F5F7FB")
PDF_DARK    = HexColor("#1A202C")
PDF_GRAY    = HexColor("#555F70")
PDF_WHITE   = HexColor("#FFFFFF")

# ─── 슬라이드 데이터 ─────────────────────────────────────
SLIDES = [
    {
        "type": "title",
        "title": "AI News Curator",
        "subtitle": "AI 기술 콘텐츠 자동 큐레이션 서비스",
        "meta": "Spring Boot · Gemini AI · Telegram Bot\n2026.04",
    },
    {
        "type": "content",
        "title": "프로젝트 개요",
        "tagline": "넘쳐나는 AI 뉴스, 정말 중요한 것만",
        "bullets": [
            ("문제", "매일 쏟아지는 AI 뉴스·블로그 중 실제로 가치 있는 글을 찾기 어려움"),
            ("해결", "여러 소스에서 자동 수집 → AI가 품질 평가 → 상위 3개를 텔레그램으로 발송"),
            ("주기", "2시간마다 자동 실행되는 완전 자동화 파이프라인"),
            ("결과", "하루에 받아보는 큐레이팅된 AI 인사이트"),
        ],
    },
    {
        "type": "content",
        "title": "주요 기능",
        "tagline": "수집 · 평가 · 요약 · 발송 — End to End",
        "bullets": [
            ("멀티 소스 수집", "Hacker News, Reddit, 국내 6개 기술 블로그 RSS"),
            ("AI 품질 평가", "Gemini로 실용성·신선도·깊이 점수화"),
            ("AI 요약", "선정된 기사를 2-3줄 한국어로 자동 요약"),
            ("웹 대시보드", "Thymeleaf 기반 5개 페이지 — 오늘/히스토리/통계"),
            ("JWT 인증 API", "회원가입·로그인·기사 조회 REST API"),
        ],
    },
    {
        "type": "pipeline",
        "title": "시스템 아키텍처",
        "tagline": "7단계 파이프라인",
        "steps": [
            ("1. COLLECT",   "HN · Reddit · 국내 기술 블로그"),
            ("2. DEDUP",     "DB에서 기존 URL 확인 · 중복 제거"),
            ("3. FILTER",    "AI/Backend 키워드 룰 필터"),
            ("4. EVALUATE",  "Gemini로 3개 축 점수화"),
            ("5. SELECT",    "종합 점수 상위 3개 선정"),
            ("6. SUMMARIZE", "Gemini로 한국어 요약 생성"),
            ("7. SEND",      "Telegram Bot으로 발송"),
        ],
    },
    {
        "type": "stack",
        "title": "기술 스택",
        "tagline": "Java 백엔드 풀스택",
        "groups": [
            ("Backend",    ["Java 17", "Spring Boot 3.4", "Spring Security", "JPA / Hibernate"]),
            ("AI",         ["Google Gemini", "gemini-2.5-flash-lite"]),
            ("Database",   ["H2 (dev)", "PostgreSQL 16 (prod)"]),
            ("Frontend",   ["Thymeleaf", "Layout Dialect", "반응형 CSS"]),
            ("Parsing",    ["Rome 2.1 (RSS)", "Jsoup 1.18 (HTML)"]),
            ("Auth",       ["JWT (jjwt 0.12)"]),
            ("DevOps",     ["Docker", "Docker Compose", "GitHub Actions CI/CD"]),
            ("Test",       ["JUnit 5", "Mockito", "Spring Boot Test"]),
        ],
    },
    {
        "type": "content",
        "title": "콘텐츠 소스",
        "tagline": "글로벌 + 국내 기술 블로그",
        "bullets": [
            ("Hacker News",  "Algolia Search API · upvote 기반 인기 글"),
            ("Reddit",       "r/artificial · r/MachineLearning RSS"),
            ("국내 블로그",  "네이버 D2 · 카카오 · 쿠팡 · 우아한형제들 · 라인 · 당근"),
            ("확장성",       "Collector 인터페이스 추가만으로 새 소스 지원"),
        ],
    },
    {
        "type": "score",
        "title": "AI 평가 로직",
        "tagline": "Gemini가 3개 축으로 점수화 → 가중합으로 종합 점수",
        "axes": [
            ("실용성", "40%", "실제 업무·개발에 적용 가능한가?"),
            ("신선도", "30%", "최신 트렌드 · 새로운 정보인가?"),
            ("깊이",   "30%", "표면적 소개를 넘어 인사이트가 있는가?"),
        ],
        "footer": "각 축 0~10점 · Top 3 선정 후 한국어 요약 생성",
    },
    {
        "type": "content",
        "title": "웹 대시보드",
        "tagline": "Thymeleaf 기반 5개 페이지",
        "bullets": [
            ("/",                  "오늘의 큐레이팅 기사 — 시간대별 그룹"),
            ("/history",           "날짜별 기사 조회 · 달력 네비게이션"),
            ("/articles/{id}",     "AI 요약 · 점수 상세 · 원문 링크"),
            ("/admin/pipeline",    "수동 실행 · 상태 확인 · 최근 로그"),
            ("/admin/stats",       "소스별 수집량 · 평균 점수 · API 사용량"),
        ],
    },
    {
        "type": "content",
        "title": "API & 인증",
        "tagline": "JWT 기반 REST API",
        "bullets": [
            ("Auth",      "POST /api/auth/{signup, login, refresh}"),
            ("Articles",  "GET /api/articles/{today, history, {id}, dates}"),
            ("Pipeline",  "POST /api/pipeline/run · GET /api/pipeline/status"),
            ("Health",    "GET /actuator/health — 컨테이너 헬스체크"),
            ("보안",      "Spring Security · BCrypt · JWT Access/Refresh"),
        ],
    },
    {
        "type": "content",
        "title": "테스트 & 품질",
        "tagline": "총 14개 테스트 클래스 — Unit · Integration · Collector",
        "bullets": [
            ("Unit",        "Filter · Selector · Evaluator · Formatter"),
            ("Integration", "ArticleRepo · Auth · Article · Pipeline Controller"),
            ("Collector",   "HN · Reddit · TechBlog (외부 API 모킹)"),
            ("CI",          "GitHub Actions에서 PR 마다 자동 실행"),
        ],
    },
    {
        "type": "content",
        "title": "배포 & 운영",
        "tagline": "Docker 컨테이너로 어디서든 동일하게",
        "bullets": [
            ("Dockerfile",    "Gradle 빌드 → JRE 런타임 멀티스테이지"),
            ("docker-compose","앱 + PostgreSQL 한 번에 기동"),
            ("Profile",       "dev(H2) / prod(PostgreSQL) 분리"),
            ("CI/CD",         "GitHub Actions: 빌드 → 테스트 → 이미지"),
            ("스케줄러",      "Spring @Scheduled · 2시간 주기 자동 실행"),
        ],
    },
    {
        "type": "closing",
        "title": "Thank You",
        "subtitle": "AI News Curator",
        "meta": "github.com/jsj1215  ·  질문 환영합니다",
    },
]

# ════════════════════════════════════════════════════════
# PPTX 생성
# ════════════════════════════════════════════════════════
def make_pptx(path: str):
    prs = Presentation()
    prs.slide_width  = Inches(13.333)
    prs.slide_height = Inches(7.5)
    SW, SH = prs.slide_width, prs.slide_height
    blank = prs.slide_layouts[6]

    KR_FONT = "Apple SD Gothic Neo"

    def add_rect(slide, x, y, w, h, color, line=False):
        s = slide.shapes.add_shape(MSO_SHAPE.RECTANGLE, x, y, w, h)
        s.fill.solid()
        s.fill.fore_color.rgb = color
        if not line:
            s.line.fill.background()
        else:
            s.line.color.rgb = color
        s.shadow.inherit = False
        return s

    def add_text(slide, x, y, w, h, text, size=18, bold=False,
                 color=DARK_TEXT, align=PP_ALIGN.LEFT, font=KR_FONT):
        tb = slide.shapes.add_textbox(x, y, w, h)
        tf = tb.text_frame
        tf.word_wrap = True
        tf.margin_left = tf.margin_right = 0
        tf.margin_top = tf.margin_bottom = 0
        lines = text.split("\n")
        for i, line in enumerate(lines):
            p = tf.paragraphs[0] if i == 0 else tf.add_paragraph()
            p.alignment = align
            r = p.add_run()
            r.text = line
            r.font.name = font
            r.font.size = Pt(size)
            r.font.bold = bold
            r.font.color.rgb = color
        return tb

    def header(slide, title, tagline=None):
        # 좌측 액센트 바
        add_rect(slide, Inches(0), Inches(0), Inches(0.35), SH, NAVY)
        add_rect(slide, Inches(0.35), Inches(0), Inches(0.12), SH, ACCENT)
        # 타이틀
        add_text(slide, Inches(0.8), Inches(0.55), Inches(12), Inches(0.9),
                 title, size=34, bold=True, color=NAVY)
        if tagline:
            add_text(slide, Inches(0.8), Inches(1.45), Inches(12), Inches(0.5),
                     tagline, size=16, color=GRAY_TEXT)
        # 구분선
        line = slide.shapes.add_shape(
            MSO_SHAPE.RECTANGLE, Inches(0.8), Inches(2.05), Inches(1.2), Inches(0.05)
        )
        line.fill.solid()
        line.fill.fore_color.rgb = ACCENT
        line.line.fill.background()

    def footer(slide, page, total):
        add_text(slide, Inches(0.8), Inches(7.05), Inches(8), Inches(0.3),
                 "AI News Curator", size=10, color=GRAY_TEXT)
        add_text(slide, Inches(11.5), Inches(7.05), Inches(1.5), Inches(0.3),
                 f"{page} / {total}", size=10, color=GRAY_TEXT, align=PP_ALIGN.RIGHT)

    total = len(SLIDES)

    for idx, data in enumerate(SLIDES, start=1):
        slide = prs.slides.add_slide(blank)
        # 배경
        add_rect(slide, Inches(0), Inches(0), SW, SH, WHITE)

        t = data["type"]

        if t == "title":
            # 풀 블리드 네이비 배경
            add_rect(slide, Inches(0), Inches(0), SW, SH, NAVY)
            # 액센트 가로 바
            add_rect(slide, Inches(1.0), Inches(2.6), Inches(1.4), Inches(0.12), ACCENT)
            add_text(slide, Inches(1.0), Inches(2.85), Inches(12), Inches(1.6),
                     data["title"], size=66, bold=True, color=WHITE)
            add_text(slide, Inches(1.0), Inches(4.4), Inches(12), Inches(0.8),
                     data["subtitle"], size=24, color=RGBColor(0xCB, 0xD5, 0xE1))
            add_text(slide, Inches(1.0), Inches(6.2), Inches(12), Inches(0.9),
                     data["meta"], size=14, color=ACCENT)
            continue

        if t == "closing":
            add_rect(slide, Inches(0), Inches(0), SW, SH, NAVY)
            add_text(slide, Inches(0), Inches(2.6), SW, Inches(1.6),
                     data["title"], size=80, bold=True, color=WHITE,
                     align=PP_ALIGN.CENTER)
            add_text(slide, Inches(0), Inches(4.3), SW, Inches(0.8),
                     data["subtitle"], size=22, color=ACCENT,
                     align=PP_ALIGN.CENTER)
            add_text(slide, Inches(0), Inches(6.3), SW, Inches(0.5),
                     data["meta"], size=14, color=RGBColor(0xCB, 0xD5, 0xE1),
                     align=PP_ALIGN.CENTER)
            continue

        header(slide, data["title"], data.get("tagline"))
        footer(slide, idx, total)

        if t == "content":
            y = Inches(2.55)
            for label, body in data["bullets"]:
                # 라벨 칩
                chip_w = Inches(2.2)
                chip = add_rect(slide, Inches(0.8), y, chip_w, Inches(0.55), LIGHT_BG)
                add_text(slide, Inches(0.8), y + Inches(0.10), chip_w, Inches(0.4),
                         label, size=14, bold=True, color=NAVY,
                         align=PP_ALIGN.CENTER)
                # 본문
                add_text(slide, Inches(3.2), y + Inches(0.08), Inches(9.5), Inches(0.5),
                         body, size=16, color=DARK_TEXT)
                y += Inches(0.85)

        elif t == "pipeline":
            steps = data["steps"]
            x = Inches(0.8)
            y = Inches(2.7)
            box_w = Inches(1.72)
            box_h = Inches(1.8)
            gap = Inches(0.05)
            for i, (name, desc) in enumerate(steps):
                bx = Inches(0.8 + i * (1.72 + 0.05))
                add_rect(slide, bx, y, box_w, Inches(0.55), NAVY)
                add_text(slide, bx, y + Inches(0.13), box_w, Inches(0.4),
                         name, size=12, bold=True, color=WHITE,
                         align=PP_ALIGN.CENTER)
                add_rect(slide, bx, y + Inches(0.55), box_w, Inches(1.25), LIGHT_BG)
                add_text(slide, bx + Inches(0.1), y + Inches(0.7),
                         box_w - Inches(0.2), Inches(1.1),
                         desc, size=11, color=DARK_TEXT, align=PP_ALIGN.CENTER)
            # 결과
            add_text(slide, Inches(0.8), Inches(5.0), Inches(12), Inches(0.5),
                     "→ 2시간마다 자동 실행 · 최종적으로 Top 3 기사를 텔레그램에 전송",
                     size=14, color=GRAY_TEXT)

        elif t == "stack":
            groups = data["groups"]
            cols = 4
            cw = Inches(3.0)
            ch = Inches(1.85)
            x0 = Inches(0.8)
            y0 = Inches(2.55)
            gap_x = Inches(0.15)
            gap_y = Inches(0.2)
            for i, (cat, items) in enumerate(groups):
                row, col = divmod(i, cols)
                bx = x0 + Emu(int((cw + gap_x) * col))
                by = y0 + Emu(int((ch + gap_y) * row))
                add_rect(slide, bx, by, cw, ch, LIGHT_BG)
                # 카테고리 헤더 바
                add_rect(slide, bx, by, cw, Inches(0.05), ACCENT)
                add_text(slide, bx + Inches(0.2), by + Inches(0.15),
                         cw - Inches(0.4), Inches(0.4),
                         cat, size=14, bold=True, color=NAVY)
                add_text(slide, bx + Inches(0.2), by + Inches(0.6),
                         cw - Inches(0.4), ch - Inches(0.7),
                         "\n".join(f"· {it}" for it in items),
                         size=11, color=DARK_TEXT)

        elif t == "score":
            axes = data["axes"]
            x = Inches(0.8)
            y = Inches(2.7)
            cw = Inches(3.95)
            ch = Inches(3.2)
            gap = Inches(0.2)
            colors = [ACCENT, ACCENT2, NAVY]
            for i, (name, weight, desc) in enumerate(axes):
                bx = x + Emu(int((cw + gap) * i))
                add_rect(slide, bx, y, cw, ch, LIGHT_BG)
                add_rect(slide, bx, y, cw, Inches(0.1), colors[i])
                # 이름
                add_text(slide, bx, y + Inches(0.4), cw, Inches(0.6),
                         name, size=24, bold=True, color=NAVY,
                         align=PP_ALIGN.CENTER)
                # 가중치
                add_text(slide, bx, y + Inches(1.1), cw, Inches(1.0),
                         weight, size=46, bold=True, color=colors[i],
                         align=PP_ALIGN.CENTER)
                # 설명
                add_text(slide, bx + Inches(0.3), y + Inches(2.3),
                         cw - Inches(0.6), Inches(0.8),
                         desc, size=12, color=GRAY_TEXT,
                         align=PP_ALIGN.CENTER)
            add_text(slide, Inches(0.8), Inches(6.2), Inches(12), Inches(0.5),
                     data["footer"], size=13, color=GRAY_TEXT,
                     align=PP_ALIGN.CENTER)

    prs.save(path)


# ════════════════════════════════════════════════════════
# PDF 생성 (reportlab 직접)
# ════════════════════════════════════════════════════════
def make_pdf(path: str):
    # 한글 폰트
    font_path = "/System/Library/Fonts/Supplemental/AppleGothic.ttf"
    pdfmetrics.registerFont(TTFont("KR", font_path))
    FONT = "KR"

    # 16:9 슬라이드, points 단위
    PAGE_W, PAGE_H = 960, 540
    c = canvas.Canvas(path, pagesize=(PAGE_W, PAGE_H))

    def fill(color):
        c.setFillColor(color)

    def stroke(color):
        c.setStrokeColor(color)

    def rect(x, y, w, h, color):
        fill(color)
        c.rect(x, y, w, h, stroke=0, fill=1)

    def text(x, y, s, size=14, bold=False, color=PDF_DARK, align="left"):
        fill(color)
        c.setFont(FONT, size)
        # baseline은 y 위치 그대로 사용 (위에서 내려오는 좌표는 호출부에서 처리)
        if align == "center":
            c.drawCentredString(x, y, s)
        elif align == "right":
            c.drawRightString(x, y, s)
        else:
            c.drawString(x, y, s)

    def y_top(off):
        """위에서 내려오는 거리(off)를 PDF y로 변환"""
        return PAGE_H - off

    def header(title, tagline=None):
        # 좌측 액센트 바
        rect(0, 0, 25, PAGE_H, PDF_NAVY)
        rect(25, 0, 9, PAGE_H, PDF_ACCENT)
        text(58, y_top(60), title, size=30, color=PDF_NAVY)
        if tagline:
            text(58, y_top(90), tagline, size=14, color=PDF_GRAY)
        # 구분선
        rect(58, y_top(110), 90, 4, PDF_ACCENT)

    def footer(page, total):
        text(58, 20, "AI News Curator", size=9, color=PDF_GRAY)
        text(PAGE_W - 58, 20, f"{page} / {total}", size=9,
             color=PDF_GRAY, align="right")

    total = len(SLIDES)

    for idx, data in enumerate(SLIDES, start=1):
        # 배경 흰색
        rect(0, 0, PAGE_W, PAGE_H, PDF_WHITE)
        t = data["type"]

        if t == "title":
            rect(0, 0, PAGE_W, PAGE_H, PDF_NAVY)
            rect(72, y_top(195), 100, 8, PDF_ACCENT)
            text(72, y_top(245), data["title"], size=58,
                 color=PDF_WHITE)
            text(72, y_top(305), data["subtitle"], size=20,
                 color=HexColor("#CBD5E1"))
            for i, line in enumerate(data["meta"].split("\n")):
                text(72, y_top(450 + i * 22), line, size=12, color=PDF_ACCENT)
            c.showPage()
            continue

        if t == "closing":
            rect(0, 0, PAGE_W, PAGE_H, PDF_NAVY)
            text(PAGE_W / 2, y_top(220), data["title"], size=68,
                 color=PDF_WHITE, align="center")
            text(PAGE_W / 2, y_top(290), data["subtitle"], size=18,
                 color=PDF_ACCENT, align="center")
            text(PAGE_W / 2, y_top(450), data["meta"], size=12,
                 color=HexColor("#CBD5E1"), align="center")
            c.showPage()
            continue

        header(data["title"], data.get("tagline"))
        footer(idx, total)

        if t == "content":
            yoff = 175
            for label, body in data["bullets"]:
                rect(58, y_top(yoff + 32), 150, 36, PDF_LIGHT)
                text(58 + 75, y_top(yoff + 22), label, size=12,
                     color=PDF_NAVY, align="center")
                text(225, y_top(yoff + 22), body, size=14, color=PDF_DARK)
                yoff += 55

        elif t == "pipeline":
            steps = data["steps"]
            n = len(steps)
            x0 = 58
            available = PAGE_W - 116
            box_w = (available - (n - 1) * 6) / n
            top = 180
            for i, (name, desc) in enumerate(steps):
                bx = x0 + i * (box_w + 6)
                rect(bx, y_top(top + 35), box_w, 35, PDF_NAVY)
                text(bx + box_w / 2, y_top(top + 22), name, size=10,
                     color=PDF_WHITE, align="center")
                rect(bx, y_top(top + 130), box_w, 95, PDF_LIGHT)
                # desc 줄바꿈 단순 처리
                words = desc.split(" · ")
                for j, w in enumerate(words):
                    text(bx + box_w / 2, y_top(top + 60 + j * 18), w,
                         size=9, color=PDF_DARK, align="center")
            text(58, y_top(370),
                 "→ 2시간마다 자동 실행 · 최종적으로 Top 3 기사를 텔레그램에 전송",
                 size=12, color=PDF_GRAY)

        elif t == "stack":
            groups = data["groups"]
            cols = 4
            cw = 210
            ch = 130
            gx = 12
            gy = 14
            x0 = 58
            y0 = 175
            for i, (cat, items) in enumerate(groups):
                row, col = divmod(i, cols)
                bx = x0 + col * (cw + gx)
                by_top = y0 + row * (ch + gy)
                rect(bx, y_top(by_top + ch), cw, ch, PDF_LIGHT)
                rect(bx, y_top(by_top + 4), cw, 4, PDF_ACCENT)
                text(bx + 14, y_top(by_top + 28), cat, size=13, color=PDF_NAVY)
                for j, it in enumerate(items):
                    text(bx + 14, y_top(by_top + 50 + j * 16),
                         f"· {it}", size=10, color=PDF_DARK)

        elif t == "score":
            axes = data["axes"]
            cw = 270
            ch = 230
            gap = 15
            total_w = cw * 3 + gap * 2
            x0 = (PAGE_W - total_w) / 2
            top = 180
            colors = [PDF_ACCENT, PDF_ACCENT2, PDF_NAVY]
            for i, (name, weight, desc) in enumerate(axes):
                bx = x0 + i * (cw + gap)
                rect(bx, y_top(top + ch), cw, ch, PDF_LIGHT)
                rect(bx, y_top(top + 7), cw, 7, colors[i])
                text(bx + cw / 2, y_top(top + 50), name, size=22,
                     color=PDF_NAVY, align="center")
                text(bx + cw / 2, y_top(top + 130), weight, size=46,
                     color=colors[i], align="center")
                text(bx + cw / 2, y_top(top + 180), desc, size=11,
                     color=PDF_GRAY, align="center")
            text(PAGE_W / 2, y_top(450), data["footer"], size=12,
                 color=PDF_GRAY, align="center")

        c.showPage()

    c.save()


if __name__ == "__main__":
    here = os.path.dirname(os.path.abspath(__file__))
    pptx_path = os.path.join(here, "AI_News_Curator_발표.pptx")
    pdf_path  = os.path.join(here, "AI_News_Curator_발표.pdf")
    make_pptx(pptx_path)
    print(f"PPTX: {pptx_path}")
    make_pdf(pdf_path)
    print(f"PDF : {pdf_path}")
