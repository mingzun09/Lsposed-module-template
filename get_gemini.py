from playwright.sync_api import sync_playwright

with sync_playwright() as p:
    browser = p.chromium.launch(headless=True)
    page = browser.new_page()
    page.goto("https://share.gemini.google/zzbKRdL47bXK", wait_until="networkidle")
    print(page.inner_text("body"))
    browser.close()
