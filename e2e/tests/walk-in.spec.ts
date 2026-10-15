import { Browser, expect, Page, test } from '@playwright/test';

/** Each user gets their own browser context, like two people at two screens. */
async function signIn(browser: Browser, username: string): Promise<Page> {
  const page = await (await browser.newContext()).newPage();
  await page.goto('/');
  await page.getByLabel('Username').fill(username);
  await page.getByLabel('Password').fill('demo1234');
  await page.getByRole('button', { name: 'Sign in' }).click();
  return page;
}

test('walk-in visitor from arrival at the gate to check-out', async ({ browser }) => {
  const visitor = `Wanda Walkin ${Date.now()}`;
  const card = `E2E-${Date.now() % 1_000_000}`;
  const desk = await signIn(browser, 'desk');
  const host = await signIn(browser, 'tina');

  // The guard registers the unannounced visitor and names the employee they came to see.
  await desk.getByLabel('Visitor name').fill(visitor);
  await desk.getByLabel('Purpose').fill('Deliver lab supplies');
  await desk.getByLabel('Here to see').selectOption({ label: 'Tina Teacher' });
  await desk.getByRole('button', { name: 'Register and notify host' }).click();
  const deskRow = desk.getByRole('row', { name: visitor });
  await expect(deskRow).toContainText('Waiting for host');
  await expect(deskRow.getByRole('button', { name: 'Check in' })).toHaveCount(0);

  // The host is notified in the app and confirms the visitor is expected.
  await expect(host.getByText(`Walk-in visitor ${visitor} is at the main gate`)).toBeVisible();
  await host.getByRole('row', { name: visitor }).getByRole('button', { name: 'Confirm' }).click();

  // The gate sees the approval and issues a lanyard card.
  await expect(deskRow).toContainText('Approved');
  await deskRow.getByLabel(`Card for ${visitor}`).fill(card);
  await deskRow.getByRole('button', { name: 'Check in' }).click();
  await expect(deskRow).toContainText('On campus');
  await expect(deskRow).toContainText(card);

  // The visitor returns the card on the way out; the card no longer identifies a visit.
  const checkOut = desk.locator('form', { hasText: 'Returned card number' });
  await checkOut.getByLabel('Returned card number').fill(card);
  await checkOut.getByRole('button', { name: 'Check out' }).click();
  await expect(deskRow).toContainText('Left');

  await checkOut.getByLabel('Returned card number').fill(card);
  await checkOut.getByRole('button', { name: 'Check out' }).click();
  await expect(desk.getByRole('alert')).toHaveText(`No visitor on campus holds card ${card}`);
});
