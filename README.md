<p align="center">
  <img src="branding/scanrecall-mark.svg" width="120" alt="ScanRecall">
</p>

<h1 align="center">ScanRecall</h1>

<p align="center">
  <strong>Scan. Remember. Follow up.</strong><br>
  Privacy-first business-card and event-contact capture for Android.<br>
  <strong>by <a href="https://arivemb.com/">ArivEmb</a></strong>
</p>

---

## v0.3 beta

This branch contains the **ScanRecall v0.3 beta**.

The stable **CardKeep v0.2.1** release remains available and unchanged while ScanRecall is being tested.

## What ScanRecall does

ScanRecall is not intended to be another generic QR-code reader.

It combines:

- visible text from a business card or event badge
- standard QR/contact data when available
- event/conference context
- discussion notes
- next actions and follow-up

into one editable contact record.

### Smart Scan

Take **one photo** of a person’s business card or event badge.

ScanRecall processes the same image locally using:

1. **OCR** for visible name, company, title, email, phone, address and other printed text.
2. **QR/barcode detection** for structured or event-specific data.
3. **Conference context** if you are scanning inside Conference Mode.

Supported QR behavior in v0.3:

- **vCard QR** → contact fields are extracted
- **MECARD QR** → contact fields are extracted
- **email / telephone QR** → contact information is extracted
- **LinkedIn / profile / website QR** → URL is retained with the contact
- **opaque event badge QR** → identifier is stored as context while visible badge text is OCR-read

ScanRecall does **not** attempt to bypass proprietary event-platform access controls. If an event QR only contains an opaque attendee token, ScanRecall preserves that token and uses visible badge text instead of pretending the QR contains personal details.

## Conference Mode

Create an event once, for example:

**Embedded World 2027 · Nuremberg**

Then each person scanned inside that conference automatically inherits:

- event name
- location
- scan date
- how-we-met default
- default tags

The contact remains fully editable.

## Privacy

ScanRecall is designed to work locally.

- no ScanRecall account
- no ads
- no analytics
- no ScanRecall cloud backend
- OCR runs on the device
- QR/barcode recognition runs on the device
- no Android Internet permission
- card/badge images and ScanRecall records remain on the device

## Android identity

The visible product name in v0.3 is **ScanRecall**.

For upgrade compatibility during the beta, the Android application ID remains:

`com.madhankurukondar.cardkeep`

This is deliberate so a future approved ScanRecall release can upgrade the existing installation instead of becoming an unrelated second app.

## Product identity

ScanRecall has its own app icon.

The visual system uses the ArivEmb palette, while the product identity is separate:

**ScanRecall**  
*by ArivEmb*

The ArivEmb corporate logo is not used as the launcher icon.

## Open source

Licensed under the **Apache License 2.0**.

You may use, fork, modify and redistribute the project under the license terms. Contributions can be proposed through pull requests; public visibility does not grant write access to this repository.

## Stable release

Until v0.3 testing is complete, the existing stable build remains:

**CardKeep v0.2.1**

The v0.3 beta is intentionally developed on a separate branch:

`scanrecall-v0.3`

---

<p align="center">
  <strong>ScanRecall</strong><br>
  Remember the people you meet.<br>
  by <a href="https://arivemb.com/">ArivEmb</a>
</p>
