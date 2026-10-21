/**
 * T Tube email collector
 *
 * Script Properties bắt buộc:
 * - SPREADSHEET_ID: ID của Google Sheet lưu email
 * - ADMIN_TOKEN: chuỗi bí mật dài, chỉ chủ dự án biết
 *
 * Deploy: Web app -> Execute as Me -> Who has access: Anyone
 */

const TTUBE_SHEET_NAME = 'Emails';
const TTUBE_MAX_EXPORT_ROWS = 5000;

function doPost(e) {
  try {
    const email = normalizeEmail_(e && e.parameter ? e.parameter.email : '');
    const source = sanitizeText_(e && e.parameter ? e.parameter.source : 'landing-page', 80);
    const consent = String(e && e.parameter ? e.parameter.consent : '').toLowerCase() === 'yes' ? 'yes' : 'no';

    if (!isValidEmail_(email)) {
      return jsonOutput_({ ok: false, error: 'invalid_email' });
    }

    if (consent !== 'yes') {
      return jsonOutput_({ ok: false, error: 'consent_required' });
    }

    const lock = LockService.getScriptLock();
    if (!lock.tryLock(5000)) {
      return jsonOutput_({ ok: false, error: 'busy' });
    }

    try {
      const sheet = getSheet_();
      const lastRow = sheet.getLastRow();

      if (lastRow > 1) {
        const emails = sheet.getRange(2, 2, lastRow - 1, 1).getDisplayValues();
        const duplicate = emails.some(function(row) {
          return normalizeEmail_(row[0]) === email;
        });

        if (duplicate) {
          return jsonOutput_({ ok: true, duplicate: true });
        }
      }

      sheet.appendRow([new Date(), email, source, consent]);
      return jsonOutput_({ ok: true, duplicate: false });
    } finally {
      lock.releaseLock();
    }
  } catch (error) {
    console.error(error);
    return jsonOutput_({ ok: false, error: 'server_error' });
  }
}

function doGet(e) {
  const params = e && e.parameter ? e.parameter : {};
  const action = String(params.action || 'status').toLowerCase();

  if (action === 'status') {
    return outputForGet_(params, { ok: true, service: 'ttube-email-collector' });
  }

  if (!isAdminTokenValid_(params.token)) {
    return outputForGet_(params, { ok: false, error: 'unauthorized' });
  }

  if (action === 'list') {
    return outputForGet_(params, {
      ok: true,
      records: listRecords_()
    });
  }

  if (action === 'csv') {
    return csvOutput_();
  }

  return outputForGet_(params, { ok: false, error: 'unknown_action' });
}

function getSheet_() {
  const props = PropertiesService.getScriptProperties();
  const spreadsheetId = String(props.getProperty('SPREADSHEET_ID') || '').trim();

  if (!spreadsheetId) {
    throw new Error('Missing SPREADSHEET_ID script property');
  }

  const spreadsheet = SpreadsheetApp.openById(spreadsheetId);
  let sheet = spreadsheet.getSheetByName(TTUBE_SHEET_NAME);

  if (!sheet) {
    sheet = spreadsheet.insertSheet(TTUBE_SHEET_NAME);
  }

  if (sheet.getLastRow() === 0) {
    sheet.appendRow(['Thời gian', 'Email', 'Nguồn', 'Đồng ý nhận tin']);
    sheet.setFrozenRows(1);
    sheet.getRange(1, 1, 1, 4).setFontWeight('bold');
    sheet.autoResizeColumns(1, 4);
  }

  return sheet;
}

function listRecords_() {
  const sheet = getSheet_();
  const lastRow = sheet.getLastRow();

  if (lastRow <= 1) {
    return [];
  }

  const startRow = Math.max(2, lastRow - TTUBE_MAX_EXPORT_ROWS + 1);
  const rowCount = lastRow - startRow + 1;
  const values = sheet.getRange(startRow, 1, rowCount, 4).getValues();

  return values.reverse().map(function(row) {
    return {
      timestamp: row[0] instanceof Date ? row[0].toISOString() : String(row[0] || ''),
      email: String(row[1] || ''),
      source: String(row[2] || ''),
      consent: String(row[3] || '')
    };
  });
}

function csvOutput_() {
  const records = listRecords_();
  const lines = [['Thời gian', 'Email', 'Nguồn', 'Đồng ý nhận tin']];

  records.forEach(function(record) {
    lines.push([record.timestamp, record.email, record.source, record.consent]);
  });

  const csv = '\uFEFF' + lines.map(function(row) {
    return row.map(csvCell_).join(',');
  }).join('\r\n');

  return ContentService
    .createTextOutput(csv)
    .setMimeType(ContentService.MimeType.CSV);
}

function outputForGet_(params, payload) {
  const callback = String(params.callback || '').trim();

  if (callback && /^[A-Za-z_$][0-9A-Za-z_$]*$/.test(callback)) {
    return ContentService
      .createTextOutput(callback + '(' + JSON.stringify(payload) + ');')
      .setMimeType(ContentService.MimeType.JAVASCRIPT);
  }

  return jsonOutput_(payload);
}

function jsonOutput_(payload) {
  return ContentService
    .createTextOutput(JSON.stringify(payload))
    .setMimeType(ContentService.MimeType.JSON);
}

function isAdminTokenValid_(token) {
  const expected = String(PropertiesService.getScriptProperties().getProperty('ADMIN_TOKEN') || '');
  const actual = String(token || '');

  if (!expected || !actual || expected.length !== actual.length) {
    return false;
  }

  let diff = 0;
  for (let i = 0; i < expected.length; i++) {
    diff |= expected.charCodeAt(i) ^ actual.charCodeAt(i);
  }
  return diff === 0;
}

function normalizeEmail_(value) {
  return String(value || '').trim().toLowerCase().slice(0, 254);
}

function isValidEmail_(email) {
  return /^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/.test(email);
}

function sanitizeText_(value, maxLength) {
  return String(value || '')
    .replace(/[\u0000-\u001F\u007F]/g, '')
    .trim()
    .slice(0, maxLength);
}

function csvCell_(value) {
  return '"' + String(value == null ? '' : value).replace(/"/g, '""') + '"';
}
