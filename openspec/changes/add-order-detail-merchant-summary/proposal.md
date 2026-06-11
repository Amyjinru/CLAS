# Add Order Detail Merchant Summary

## Why

Order detail currently shows the order and items, but it does not surface the merchant context. Meituan-style order detail keeps the merchant name and contact/address visible so users can orient quickly from notifications and history.

## What Changes

- Load the merchant associated with the order detail.
- Show merchant name, category, score, phone, and address when available.
- Keep order detail usable if the merchant lookup fails.

## Non-Goals

- No backend API changes in this slice.
- No merchant chat or phone-call integration in this slice.
