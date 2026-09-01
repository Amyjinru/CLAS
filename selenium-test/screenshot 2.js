// 截图专用版本 —— 浏览器会停留 10 秒等你截图
const {By, Builder, Browser} = require('selenium-webdriver');

(async function () {
  let driver = await new Builder().forBrowser('chrome').build();

  try {
    await driver.get('https://www.selenium.dev/selenium/web/web-form.html');

    let title = await driver.getTitle();
    console.log('页面标题:', title);

    await driver.manage().setTimeouts({implicit: 500});

    let textBox = await driver.findElement(By.name('my-text'));
    let submitButton = await driver.findElement(By.css('button'));

    await textBox.sendKeys('Selenium');
    await submitButton.click();

    let message = await driver.findElement(By.id('message'));
    let value = await message.getText();
    console.log('消息内容:', value);

    // 停留 10 秒给你截图
    console.log('>>> 浏览器将在 10 秒后关闭，请现在截图！ <<<');
    await driver.sleep(10000);
  } finally {
    await driver.quit();
    console.log('浏览器已关闭');
  }
})();
