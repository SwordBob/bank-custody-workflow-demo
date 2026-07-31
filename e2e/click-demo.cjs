// 用 Playwright 真实点击 demo.html 4 个 Tab 的按钮, 抓 console 错误和 4xx/5xx 响应
const { chromium } = require('/opt/homebrew/lib/node_modules/playwright');
const { spawnSync } = require('child_process');

// tab key (业务流程名) → HTML 元素 id 前缀. 跟 demo.html 里的 TAB_PREFIX 一致
// (HTML 用短前缀 post/clear/rec/val, tab key 是 posting/clearing/...)
const TAB_PREFIX = {
  posting: 'post',
  clearing: 'clear',
  reconciliation: 'rec',
  valuation: 'val'
};

// 服务重启后 MySQL 持久化, act_ru_task 里有历史 task (VCH-DEMO-001 等), demoRun 会
// 拉到这些旧 task 调 complete → INSERT voucher UNIQUE 冲突 → 500. 跑前先清掉.
function cleanupHistoricalTasks() {
  const sql = "DELETE FROM act_ru_task WHERE assignee_ IN ('张三', '李四', '王五', '赵六');";
  const res = spawnSync('/usr/local/mysql/bin/mysql',
    ['-uroot', '--socket=/tmp/mysql.sock', 'bank_custody_demo', '-e', sql],
    { encoding: 'utf8' });
  if (res.status !== 0) {
    console.log('  ⚠️  清历史 task 失败 (继续跑):', res.stderr.trim().split('\n')[0]);
  } else {
    console.log('  🧹 已清历史 task (assignee IN 张三/李四/王五/赵六)');
  }
}

(async () => {
  cleanupHistoricalTasks();

  const browser = await chromium.launch({ headless: true });
  const context = await browser.newContext();
  const page = await context.newPage();

  const consoleErrors = [];
  const pageErrors = [];
  const failedResponses = [];

  page.on('console', msg => {
    if (msg.type() === 'error') consoleErrors.push(msg.text());
  });
  page.on('pageerror', err => {
    pageErrors.push(err.message);
  });
  page.on('response', resp => {
    if (resp.status() >= 400) {
      failedResponses.push(resp.status() + ' ' + resp.url());
    }
  });

  console.log('==== 打开 demo.html ====');
  await page.goto('http://127.0.0.1:8080/demo.html', { waitUntil: 'networkidle', timeout: 20000 });
  await page.waitForSelector('.tab', { timeout: 5000 });
  console.log('  ✅ 页面加载完成, title:', await page.title());

  const tabs = ['posting', 'clearing', 'reconciliation', 'valuation'];

  for (const tab of tabs) {
    console.log('\n==== Tab: ' + tab + ' ====');

    // 切 Tab
    await page.click('.tab[data-tab="' + tab + '"]');
    await page.waitForTimeout(800);
    console.log('  ✅ 切到 ' + tab + ' Tab');

    // 关键: 限定到当前 panel (display: block 那个), 避开其他 Tab 隐藏的同名按钮
    const btn = page.locator('#panel-' + tab + ' button.btn.success:has-text("一键演示")');
    await btn.click({ timeout: 5000 });
    console.log('  ✅ 已点一键演示');

    // 等 8s 让 demoRun 跑完 (内部循环 5 轮, 每轮 0.4s + start)
    await page.waitForTimeout(8000);

    // 截图
    const screenshot = '/tmp/tab-' + tab + '.png';
    await page.screenshot({ path: screenshot, fullPage: true });
    console.log('  📸 ' + screenshot);

    // 用 JS 读 status, 不依赖 locator strict mode. 跟 demo.html TAB_PREFIX 同步
    const status = await page.evaluate(({ t, prefix }) => {
      const el = document.getElementById(prefix + '-status');
      return el ? el.textContent.trim() : 'NO_STATUS';
    }, { t: tab, prefix: TAB_PREFIX[tab] });
    const shortStatus = status.replace(/\n/g, ' | ').substring(0, 200);
    console.log('  📋 Status: ' + shortStatus);

    // 业务表最新一条
    const tableInfo = await page.evaluate(({ t, prefix }) => {
      const el = document.getElementById(prefix + '-tasks');
      if (!el) return 'no tasks panel';
      const taskCount = el.querySelectorAll('.task-card').length;
      return '待办 task 数=' + taskCount;
    }, { t: tab, prefix: TAB_PREFIX[tab] });
    console.log('  📊 ' + tableInfo);
  }

  console.log('\n==== ==== 总结 ==== ====');
  console.log('Console errors:', consoleErrors.length);
  consoleErrors.forEach(e => console.log('  ❌ ' + e));
  console.log('Page errors:', pageErrors.length);
  pageErrors.forEach(e => console.log('  ❌ ' + e));
  console.log('Failed responses (4xx/5xx):', failedResponses.length);
  failedResponses.forEach(r => console.log('  ❌ ' + r));

  await browser.close();
  const total = consoleErrors.length + pageErrors.length + failedResponses.length;
  process.exit(total > 0 ? 1 : 0);
})().catch(e => {
  console.error('FATAL:', e.message, e.stack);
  process.exit(2);
});
