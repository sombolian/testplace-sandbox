"""Build the Coupa -> Zip migration deck for Procurement and FP&A."""
from pptx import Presentation
from pptx.util import Inches, Pt, Emu
from pptx.dml.color import RGBColor
from pptx.enum.shapes import MSO_SHAPE
from pptx.enum.text import PP_ALIGN, MSO_ANCHOR
from pathlib import Path

IMG_DIR = Path("/tmp/zip_images")
OUT = Path("/home/user/testplace-sandbox/Coupa_to_Zip_Migration.pptx")

# Brand palette
NAVY = RGBColor(0x0B, 0x1F, 0x3A)
ZIP_GREEN = RGBColor(0x00, 0xC2, 0x6E)
ACCENT = RGBColor(0xFF, 0x6B, 0x35)
LIGHT = RGBColor(0xF4, 0xF6, 0xFA)
GRAY = RGBColor(0x55, 0x5B, 0x6E)
WHITE = RGBColor(0xFF, 0xFF, 0xFF)
DARK = RGBColor(0x12, 0x18, 0x28)

prs = Presentation()
prs.slide_width = Inches(13.333)
prs.slide_height = Inches(7.5)
SW, SH = prs.slide_width, prs.slide_height
BLANK = prs.slide_layouts[6]


def add_rect(slide, x, y, w, h, fill, line=None):
    shape = slide.shapes.add_shape(MSO_SHAPE.RECTANGLE, x, y, w, h)
    shape.fill.solid()
    shape.fill.fore_color.rgb = fill
    if line is None:
        shape.line.fill.background()
    else:
        shape.line.color.rgb = line
    shape.shadow.inherit = False
    return shape


def add_text(slide, x, y, w, h, text, *, size=18, bold=False, color=DARK,
             align=PP_ALIGN.LEFT, anchor=MSO_ANCHOR.TOP, font="Calibri"):
    tb = slide.shapes.add_textbox(x, y, w, h)
    tf = tb.text_frame
    tf.word_wrap = True
    tf.margin_left = Emu(0)
    tf.margin_right = Emu(0)
    tf.margin_top = Emu(0)
    tf.margin_bottom = Emu(0)
    tf.vertical_anchor = anchor
    lines = text.split("\n") if isinstance(text, str) else text
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


def add_bullets(slide, x, y, w, h, items, *, size=16, color=DARK, bold_first=False):
    tb = slide.shapes.add_textbox(x, y, w, h)
    tf = tb.text_frame
    tf.word_wrap = True
    for i, item in enumerate(items):
        p = tf.paragraphs[0] if i == 0 else tf.add_paragraph()
        p.alignment = PP_ALIGN.LEFT
        p.space_after = Pt(6)
        r = p.add_run()
        r.text = "•  " + item
        r.font.name = "Calibri"
        r.font.size = Pt(size)
        r.font.color.rgb = color
        r.font.bold = bold_first and i == 0
    return tb


def slide_header(slide, title, subtitle=None):
    # Top color band
    add_rect(slide, 0, 0, SW, Inches(0.9), NAVY)
    add_rect(slide, 0, Inches(0.9), SW, Inches(0.05), ZIP_GREEN)
    add_text(slide, Inches(0.5), Inches(0.18), Inches(12.3), Inches(0.6),
             title, size=26, bold=True, color=WHITE)
    if subtitle:
        add_text(slide, Inches(0.5), Inches(1.05), Inches(12.3), Inches(0.4),
                 subtitle, size=14, color=GRAY)


def footer(slide, page):
    add_text(slide, Inches(0.5), Inches(7.1), Inches(8), Inches(0.3),
             "Procurement Transformation  |  Coupa → Zip", size=10, color=GRAY)
    add_text(slide, Inches(12.5), Inches(7.1), Inches(0.7), Inches(0.3),
             str(page), size=10, color=GRAY, align=PP_ALIGN.RIGHT)


# ---------- Slide 1: Title ----------
s = prs.slides.add_slide(BLANK)
add_rect(s, 0, 0, SW, SH, NAVY)
# accent stripe
add_rect(s, 0, Inches(3.2), SW, Inches(0.08), ZIP_GREEN)
add_text(s, Inches(0.8), Inches(2.0), Inches(11.5), Inches(1.0),
         "From Coupa to Zip", size=54, bold=True, color=WHITE)
add_text(s, Inches(0.8), Inches(3.45), Inches(11.5), Inches(0.6),
         "Modernizing Vendor Onboarding & Procurement Orchestration",
         size=24, color=ZIP_GREEN)
add_text(s, Inches(0.8), Inches(4.2), Inches(11.5), Inches(0.5),
         "Briefing for Procurement & FP&A Teams", size=18, color=WHITE)
add_text(s, Inches(0.8), Inches(6.6), Inches(11.5), Inches(0.4),
         "Prepared by: Procurement Transformation Office", size=12, color=LIGHT)

# ---------- Slide 2: Why we are deprecating Coupa ----------
s = prs.slides.add_slide(BLANK)
slide_header(s, "Why we are deprecating Coupa",
             "Coupa has served us, but it no longer fits how we buy today")
add_text(s, Inches(0.5), Inches(1.6), Inches(6.0), Inches(0.5),
         "Where Coupa is holding us back", size=18, bold=True, color=NAVY)
add_bullets(s, Inches(0.5), Inches(2.1), Inches(6.0), Inches(5.0), [
    "Heavy, IT-dependent configuration — every workflow change is a project",
    "Low employee adoption: requesters bypass the system, creating maverick spend",
    "Fragmented intake — separate tools for legal, security, finance reviews",
    "Manual vendor onboarding with limited automated risk checks",
    "Limited visibility into request status for FP&A and budget owners",
    "Total cost of ownership (license + admin) growing faster than value",
], size=15)

add_rect(s, Inches(7.0), Inches(1.6), Inches(5.8), Inches(5.5), LIGHT)
add_text(s, Inches(7.25), Inches(1.75), Inches(5.5), Inches(0.5),
         "What we need instead", size=18, bold=True, color=NAVY)
add_bullets(s, Inches(7.25), Inches(2.3), Inches(5.4), Inches(4.7), [
    "A single, intuitive front door for every purchase request",
    "No-code workflows owned by Procurement, not IT",
    "AI-powered vendor risk, compliance and data validation",
    "Real-time spend visibility for FP&A and budget owners",
    "Native integrations with our ERP, contract and AP systems",
    "Faster cycle times so the business stops working around us",
], size=15)
footer(s, 2)

# ---------- Slide 3: Why Zip ----------
s = prs.slides.add_slide(BLANK)
slide_header(s, "Why Zip", "The AI-powered procurement orchestration platform")

# Left: image
s.shapes.add_picture(str(IMG_DIR / "supplier_onboarding_hero.png"),
                     Inches(0.5), Inches(1.6), Inches(6.2), Inches(4.2))

# Right: value props
add_text(s, Inches(7.0), Inches(1.6), Inches(5.8), Inches(0.5),
         "What Zip gives us", size=18, bold=True, color=NAVY)
add_bullets(s, Inches(7.0), Inches(2.1), Inches(5.8), Inches(5.0), [
    "Single intake portal — one place for any request, from any team",
    "No-code workflow engine owned by Procurement",
    "AI agents that pre-screen suppliers, contracts and risk",
    "1.2M vendors managed and 6M approvals orchestrated on the platform",
    "Native ERP, contract and AP integrations — no double entry",
    "Trusted by Lyft, Snowflake, Reddit, Sephora, Canva and Northwestern Mutual",
], size=14)

# KPI strip
y = Inches(6.05)
kpis = [
    ("85%", "Faster onboarding cycle time"),
    ("98%", "Supplier portal completion rate"),
    ("2x",  "Supplier risk coverage"),
    ("50%+", "Reduction in approval cycle time"),
]
bw = Inches(2.95); gap = Inches(0.15); start = Inches(0.5)
for i, (k, v) in enumerate(kpis):
    x = start + (bw + gap) * i
    add_rect(s, x, y, bw, Inches(0.95), ZIP_GREEN)
    add_text(s, x, y + Inches(0.05), bw, Inches(0.45),
             k, size=24, bold=True, color=WHITE, align=PP_ALIGN.CENTER)
    add_text(s, x, y + Inches(0.5), bw, Inches(0.4),
             v, size=11, color=WHITE, align=PP_ALIGN.CENTER)
footer(s, 3)

# ---------- Slide 4: Coupa vs Zip side-by-side ----------
s = prs.slides.add_slide(BLANK)
slide_header(s, "Coupa vs. Zip — at a glance")

rows = [
    ("Dimension",            "Coupa (today)",                              "Zip (target)"),
    ("Intake experience",    "Multiple forms, email, Slack workarounds",   "Single AI-guided intake portal"),
    ("Workflow changes",     "IT ticket, weeks of lead time",              "No-code, owned by Procurement"),
    ("Vendor onboarding",    "Manual data collection & checks",            "AI agents auto-collect & validate"),
    ("Risk & compliance",    "Bolt-on tools, manual reviews",              "Built-in TIN, VAT, OFAC, D&B, bank checks"),
    ("FP&A visibility",      "Lagging reports, limited drill-down",        "Real-time spend insights & dashboards"),
    ("Adoption",             "Low — requesters bypass the tool",           "High — guided, self-service for everyone"),
]

x0 = Inches(0.5); y0 = Inches(1.6)
col_w = [Inches(2.6), Inches(5.0), Inches(5.2)]
row_h = Inches(0.7)

# header row
for i, txt in enumerate(rows[0]):
    x = x0 + sum(col_w[:i], Emu(0))
    add_rect(s, x, y0, col_w[i], row_h, NAVY)
    add_text(s, x + Inches(0.1), y0, col_w[i] - Inches(0.2), row_h,
             txt, size=14, bold=True, color=WHITE, anchor=MSO_ANCHOR.MIDDLE)

# body rows
for r, row in enumerate(rows[1:], start=1):
    y = y0 + row_h * r
    bg = LIGHT if r % 2 else WHITE
    for i, txt in enumerate(row):
        x = x0 + sum(col_w[:i], Emu(0))
        add_rect(s, x, y, col_w[i], row_h, bg)
        col_color = NAVY if i == 0 else (GRAY if i == 1 else DARK)
        bold = i == 0
        add_text(s, x + Inches(0.12), y, col_w[i] - Inches(0.24), row_h,
                 txt, size=13, bold=bold, color=col_color, anchor=MSO_ANCHOR.MIDDLE)
footer(s, 4)

# ---------- Slide 5: Vendor onboarding journey overview ----------
s = prs.slides.add_slide(BLANK)
slide_header(s, "Vendor onboarding in Zip",
             "One guided journey — from request to ERP-ready supplier")

s.shapes.add_picture(str(IMG_DIR / "vendor_header.png"),
                     Inches(0.5), Inches(1.55), Inches(12.3), Inches(3.2))

# 5 step pills
steps = ["1. Intake request", "2. Risk & compliance",
         "3. Cross-functional review", "4. Contract & terms", "5. ERP sync"]
pill_w = Inches(2.40); pill_h = Inches(0.55); gap = Inches(0.07)
total = pill_w * len(steps) + gap * (len(steps) - 1)
start = (SW - total) / 2
y = Inches(5.0)
for i, label in enumerate(steps):
    x = start + (pill_w + gap) * i
    shape = s.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, x, y, pill_w, pill_h)
    shape.fill.solid()
    shape.fill.fore_color.rgb = ZIP_GREEN if i % 2 == 0 else NAVY
    shape.line.fill.background()
    tf = shape.text_frame
    tf.margin_left = Emu(0); tf.margin_right = Emu(0)
    tf.vertical_anchor = MSO_ANCHOR.MIDDLE
    p = tf.paragraphs[0]
    p.alignment = PP_ALIGN.CENTER
    r = p.add_run(); r.text = label
    r.font.size = Pt(13); r.font.bold = True; r.font.color.rgb = WHITE

add_text(s, Inches(0.5), Inches(5.85), Inches(12.3), Inches(1.2),
         "Each step is automated and tracked. Requesters get a single status view; "
         "Procurement, Legal, Security, IT and FP&A reviewers are pulled in only when "
         "their input is needed. Suppliers complete every requirement through a single "
         "branded portal — no email back-and-forth.",
         size=14, color=GRAY)
footer(s, 5)

# ---------- Slide 6: Step 1 — Intake ----------
s = prs.slides.add_slide(BLANK)
slide_header(s, "Step 1 — Intake: a single front door",
             "Requesters describe what they need; Zip figures out the rest")

add_bullets(s, Inches(0.5), Inches(1.7), Inches(6.0), Inches(5.0), [
    "Guided, conversational intake form — no procurement jargon",
    "AI suggests the right category, owner and reviewers automatically",
    "Detects duplicate suppliers and existing contracts before a new request opens",
    "Pulls budget owner and GL coding from the ERP at the point of request",
    "Auto-routes to Procurement, Legal, Security, IT, Privacy and FP&A in parallel",
    "Requester sees a single status timeline end-to-end",
], size=15)

s.shapes.add_picture(str(IMG_DIR / "precheck_agent.png"),
                     Inches(7.0), Inches(1.7), Inches(5.8), Inches(4.5))
add_text(s, Inches(7.0), Inches(6.3), Inches(5.8), Inches(0.4),
         "Zip Procurement Precheck Agent screening incoming requests",
         size=11, color=GRAY, align=PP_ALIGN.CENTER)
footer(s, 6)

# ---------- Slide 7: Step 2 — Vendor data collection ----------
s = prs.slides.add_slide(BLANK)
slide_header(s, "Step 2 — Supplier data collection via the Zip portal",
             "The vendor does the work, in one place, once")

s.shapes.add_picture(str(IMG_DIR / "supplier360.png"),
                     Inches(0.5), Inches(1.7), Inches(6.2), Inches(4.5))
add_text(s, Inches(0.5), Inches(6.3), Inches(6.2), Inches(0.4),
         "Zip Supplier 360 — a unified vendor record",
         size=11, color=GRAY, align=PP_ALIGN.CENTER)

add_text(s, Inches(7.0), Inches(1.7), Inches(5.8), Inches(0.5),
         "What the supplier provides", size=18, bold=True, color=NAVY)
add_bullets(s, Inches(7.0), Inches(2.2), Inches(5.8), Inches(5.0), [
    "Legal entity, tax IDs (W-9 / W-8), VAT, addresses",
    "Banking details with automated bank verification",
    "Diversity certifications and ESG attestations",
    "Insurance certificates and SOC 2 / ISO documentation",
    "DPA, MSA, NDA — pulled in for Legal review",
    "Security questionnaires routed to the IT Security team",
], size=14)
footer(s, 7)

# ---------- Slide 8: Step 3 — Risk & compliance ----------
s = prs.slides.add_slide(BLANK)
slide_header(s, "Step 3 — Automated risk & compliance checks",
             "AI agents run the checks we used to do by hand")

# 3 cards
cards = [
    ("Supplier 360 Agent",
     "Analyzes supplier documents, external data sources and news to surface risks before approval."),
    ("Procurement Precheck Agent",
     "Reviews MSAs, DPAs, SOC 2 and other documents to flag legal and security risks early."),
    ("Adverse Media Agent",
     "Scans public news and watchlists for any reputational or sanctions risk on the supplier."),
]
cw = Inches(4.0); ch = Inches(2.6); gap = Inches(0.25)
start = (SW - (cw * 3 + gap * 2)) / 2
y = Inches(1.7)
for i, (title, body) in enumerate(cards):
    x = start + (cw + gap) * i
    add_rect(s, x, y, cw, ch, LIGHT)
    add_rect(s, x, y, cw, Inches(0.1), ZIP_GREEN)
    add_text(s, x + Inches(0.25), y + Inches(0.25), cw - Inches(0.5), Inches(0.5),
             title, size=16, bold=True, color=NAVY)
    add_text(s, x + Inches(0.25), y + Inches(0.85), cw - Inches(0.5), ch - Inches(1.0),
             body, size=12, color=DARK)

add_text(s, Inches(0.5), Inches(4.55), Inches(12.3), Inches(0.5),
         "Automated third-party checks at every onboarding:",
         size=15, bold=True, color=NAVY)

checks = ["TIN match", "VAT validation", "OFAC / sanctions",
          "D&B financial", "Bank account verification", "Watchlist & adverse media"]
cw2 = Inches(2.0); ch2 = Inches(0.7); gap2 = Inches(0.07)
start2 = (SW - (cw2 * 6 + gap2 * 5)) / 2
y2 = Inches(5.15)
for i, c in enumerate(checks):
    x = start2 + (cw2 + gap2) * i
    shape = s.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, x, y2, cw2, ch2)
    shape.fill.solid(); shape.fill.fore_color.rgb = NAVY
    shape.line.fill.background()
    tf = shape.text_frame; tf.vertical_anchor = MSO_ANCHOR.MIDDLE
    p = tf.paragraphs[0]; p.alignment = PP_ALIGN.CENTER
    r = p.add_run(); r.text = c
    r.font.size = Pt(12); r.font.bold = True; r.font.color.rgb = WHITE

s.shapes.add_picture(str(IMG_DIR / "adverse_media.png"),
                     Inches(3.5), Inches(6.1), Inches(6.3), Inches(1.2))
footer(s, 8)

# ---------- Slide 9: Step 4 + 5 review, contracts, ERP ----------
s = prs.slides.add_slide(BLANK)
slide_header(s, "Steps 4 & 5 — Reviews, contracts & ERP sync",
             "Parallel reviews, then a clean handoff to the systems of record")

# Two columns
add_rect(s, Inches(0.5), Inches(1.7), Inches(6.1), Inches(5.3), LIGHT)
add_rect(s, Inches(0.5), Inches(1.7), Inches(6.1), Inches(0.1), ZIP_GREEN)
add_text(s, Inches(0.7), Inches(1.9), Inches(5.8), Inches(0.5),
         "Step 4 — Cross-functional review", size=18, bold=True, color=NAVY)
add_bullets(s, Inches(0.7), Inches(2.5), Inches(5.8), Inches(4.4), [
    "Procurement, Legal, Security, IT, Privacy, FP&A reviews in parallel",
    "Conditional steps — reviewers only see requests that need them",
    "Built-in SLAs, reminders, escalations — no chasing in Slack",
    "Audit trail captured automatically for SOX and internal audit",
    "FP&A sees committed spend before approval, not after",
], size=14)

add_rect(s, Inches(6.7), Inches(1.7), Inches(6.1), Inches(5.3), LIGHT)
add_rect(s, Inches(6.7), Inches(1.7), Inches(6.1), Inches(0.1), ACCENT)
add_text(s, Inches(6.9), Inches(1.9), Inches(5.8), Inches(0.5),
         "Step 5 — Contracts & ERP sync", size=18, bold=True, color=NAVY)
add_bullets(s, Inches(6.9), Inches(2.5), Inches(5.8), Inches(4.4), [
    "Contract drafted and signed in our CLM, mirrored to the vendor record",
    "One-click supplier creation in the ERP — no rekeying, no duplicates",
    "Banking and tax data pushed to AP, ready for first invoice",
    "PO generated from the approved request, tied to the budget",
    "Status visible to requester, FP&A and Procurement in real time",
], size=14)
footer(s, 9)

# ---------- Slide 10: What changes for each team ----------
s = prs.slides.add_slide(BLANK)
slide_header(s, "What this means for your team")

# Procurement column
add_rect(s, Inches(0.5), Inches(1.7), Inches(6.1), Inches(5.3), WHITE, line=NAVY)
add_rect(s, Inches(0.5), Inches(1.7), Inches(6.1), Inches(0.6), NAVY)
add_text(s, Inches(0.7), Inches(1.78), Inches(5.8), Inches(0.5),
         "Procurement", size=18, bold=True, color=WHITE)
add_bullets(s, Inches(0.7), Inches(2.45), Inches(5.8), Inches(4.5), [
    "Own and edit your own workflows — no IT ticket required",
    "Automated risk checks free up time for strategic sourcing",
    "Cleaner vendor master — one record, one source of truth",
    "Stronger compliance posture with built-in audit trail",
    "Better data to negotiate with — full spend & supplier history",
], size=14)

# FP&A column
add_rect(s, Inches(6.7), Inches(1.7), Inches(6.1), Inches(5.3), WHITE, line=NAVY)
add_rect(s, Inches(6.7), Inches(1.7), Inches(6.1), Inches(0.6), ZIP_GREEN)
add_text(s, Inches(6.9), Inches(1.78), Inches(5.8), Inches(0.5),
         "FP&A", size=18, bold=True, color=WHITE)
add_bullets(s, Inches(6.9), Inches(2.45), Inches(5.8), Inches(4.5), [
    "Real-time view of committed and pending spend by department",
    "Budget checks at request time — no surprises at month-end",
    "GL coding validated upstream, fewer journal entries to fix",
    "Cleaner accruals — POs and invoices reconcile automatically",
    "Self-serve dashboards for cost center owners",
], size=14)
footer(s, 10)

# ---------- Slide 11: Migration plan ----------
s = prs.slides.add_slide(BLANK)
slide_header(s, "Migration plan", "Phased rollout — minimal disruption to the business")

phases = [
    ("Phase 1", "Discover & Design",        "Weeks 1–4",
     "Map current Coupa workflows, define future-state in Zip, identify integrations."),
    ("Phase 2", "Configure & Integrate",     "Weeks 5–10",
     "Build no-code workflows, connect ERP, CLM, SSO, AP and security tools."),
    ("Phase 3", "Pilot",                     "Weeks 11–14",
     "Pilot with one BU. Migrate active vendors, train requesters, refine workflows."),
    ("Phase 4", "Company-wide rollout",      "Weeks 15–20",
     "Cut over remaining BUs, run Coupa in parallel for 30 days, then deprecate."),
    ("Phase 5", "Deprecate Coupa",           "Week 21+",
     "Read-only archive of Coupa data, terminate licenses, realize savings."),
]
y = Inches(1.7); row_h = Inches(1.0); gap = Inches(0.08)
for i, (ph, name, when, desc) in enumerate(phases):
    yy = y + (row_h + gap) * i
    # phase badge
    add_rect(s, Inches(0.5), yy, Inches(1.4), row_h, NAVY)
    add_text(s, Inches(0.5), yy, Inches(1.4), row_h,
             ph, size=18, bold=True, color=WHITE,
             align=PP_ALIGN.CENTER, anchor=MSO_ANCHOR.MIDDLE)
    # body
    add_rect(s, Inches(1.95), yy, Inches(10.85), row_h, LIGHT)
    add_text(s, Inches(2.15), yy + Inches(0.08), Inches(8.0), Inches(0.4),
             name, size=15, bold=True, color=NAVY)
    add_text(s, Inches(2.15), yy + Inches(0.45), Inches(8.5), Inches(0.5),
             desc, size=12, color=DARK)
    add_text(s, Inches(10.6), yy + Inches(0.2), Inches(2.1), Inches(0.5),
             when, size=13, bold=True, color=ACCENT,
             align=PP_ALIGN.RIGHT, anchor=MSO_ANCHOR.MIDDLE)
footer(s, 11)

# ---------- Slide 12: Asks & next steps ----------
s = prs.slides.add_slide(BLANK)
slide_header(s, "What we need from you", "Procurement & FP&A as design partners")

add_text(s, Inches(0.5), Inches(1.7), Inches(6.0), Inches(0.5),
         "Procurement", size=18, bold=True, color=NAVY)
add_bullets(s, Inches(0.5), Inches(2.2), Inches(6.0), Inches(4.5), [
    "Nominate workstream leads for sourcing, vendor mgmt and contracts",
    "Validate the future-state intake form and approval matrix",
    "Help clean the vendor master before migration",
    "Lead requester training and change communications",
], size=14)

add_text(s, Inches(7.0), Inches(1.7), Inches(5.8), Inches(0.5),
         "FP&A", size=18, bold=True, color=NAVY)
add_bullets(s, Inches(7.0), Inches(2.2), Inches(5.8), Inches(4.5), [
    "Define the budget-check rules per cost center",
    "Confirm GL mapping and accrual logic with Accounting",
    "Review the new spend dashboards and KPIs",
    "Co-own quarterly business reviews on savings & cycle time",
], size=14)

# Next steps banner
add_rect(s, Inches(0.5), Inches(5.7), Inches(12.3), Inches(1.3), ZIP_GREEN)
add_text(s, Inches(0.8), Inches(5.8), Inches(11.7), Inches(0.5),
         "Next steps", size=18, bold=True, color=WHITE)
add_text(s, Inches(0.8), Inches(6.25), Inches(11.7), Inches(0.7),
         "1. Approve the migration plan   ·   2. Confirm workstream leads "
         "   ·   3. Kick-off workshop in 2 weeks",
         size=14, color=WHITE)
footer(s, 12)

# ---------- Slide 13: Closing ----------
s = prs.slides.add_slide(BLANK)
add_rect(s, 0, 0, SW, SH, NAVY)
add_rect(s, 0, Inches(3.3), SW, Inches(0.08), ZIP_GREEN)
add_text(s, Inches(0.8), Inches(2.4), Inches(11.5), Inches(1.0),
         "Faster. Smarter. Together.", size=48, bold=True, color=WHITE)
add_text(s, Inches(0.8), Inches(3.5), Inches(11.5), Inches(0.6),
         "Modern procurement, built for Procurement and FP&A.",
         size=22, color=ZIP_GREEN)
add_text(s, Inches(0.8), Inches(6.7), Inches(11.5), Inches(0.4),
         "Questions?  procurement-transformation@company.com",
         size=14, color=LIGHT)

prs.save(OUT)
print(f"Wrote {OUT}")
