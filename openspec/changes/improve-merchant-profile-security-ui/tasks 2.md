## 1. Audit Existing Flows

- [x] 1.1 Review `UserSettingsView.vue` account security phone-change behavior, messages, loading state, and session refresh.
- [x] 1.2 Review `MerchantProfileEditDialog.vue` current account phone, merchant contact phone, and bank account code flows.
- [x] 1.3 Confirm existing API helpers cover account phone change, merchant contact phone code, bank code, merchant profile update, and session user update.

## 2. Merchant Verification Behavior

- [x] 2.1 Refactor merchant dialog verification state into a consistent local helper or composable pattern for sending, cooldown, sent value, and reset behavior.
- [x] 2.2 Align account-bound phone code sending with the user account security `sendPhoneChangeCode` flow and validation messages.
- [x] 2.3 Ensure account-bound phone submission calls `updateBoundPhone`, refreshes session user/token, and clears stale verification state after success.
- [x] 2.4 Ensure merchant contact phone changes require a code sent for the submitted phone value and include `phoneCode` only when needed.
- [x] 2.5 Ensure merchant bank account changes require a code sent for the submitted bank value and include `bankCode` only when needed.
- [x] 2.6 Ensure basic-only merchant profile edits can save without phone or bank verification codes.

## 3. Button Layout And Styling

- [x] 3.1 Update merchant dialog code rows so inputs and send-code buttons use consistent desktop alignment, gap, button width, and control height.
- [x] 3.2 Update dialog footer actions so cancel/save buttons are grouped and aligned consistently with user settings button style.
- [x] 3.3 Update mobile styles so verification rows stack cleanly without text overflow, overlap, or uneven spacing.
- [x] 3.4 Keep styling scoped and use existing project CSS variables and Element Plus button conventions.

## 4. Verification

- [ ] 4.1 Manually verify account-bound phone change sends code, submits code, refreshes session user/token, and keeps merchant profile data visible.
- [ ] 4.2 Manually verify merchant contact phone and bank account changes require matching sent-code state, and changing the field after sending clears the code.
- [ ] 4.3 Manually verify store name, address, business hours, delivery radius, and logo-only changes do not require sensitive-field codes.
- [ ] 4.4 Run the relevant frontend checks/build and inspect the merchant profile dialog at desktop and narrow viewport widths.
