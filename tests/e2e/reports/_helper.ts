import { test, expect, loginAndNavigate } from '../fixtures';
import type { Page } from '@playwright/test';
import * as zlib from 'zlib';
import * as fs from 'fs';

export function runReportSmoke(domain: string, route: string): void {
  test.describe(`${domain} report smoke`, () => {
    test('renders page with render button and GraphQL 200', async ({ page }) => {
      const graphqlResponses: { status: number; body: string }[] = [];
      page.on('response', async (resp) => {
        if (resp.url().includes('/graphql') && resp.request().method() === 'POST') {
          let body = '';
          try {
            body = await resp.text();
          } catch {
            body = '';
          }
          graphqlResponses.push({ status: resp.status(), body });
        }
      });

      await loginAndNavigate(page, route);

      const bodyText = (await page.textContent('body')) || '';
      expect(bodyText.length, 'Report page should render').toBeGreaterThan(50);

      const renderBtn = page.locator('button:has-text("渲染"), button:has-text("Render"), button[type="submit"]').first();
      const btnExists = await renderBtn.count();
      if (btnExists > 0) {
        await renderBtn.click({ timeout: 10_000 }).catch(() => {});
        await page.waitForTimeout(3000);
      }

      expect(graphqlResponses.length, 'Should have GraphQL calls').toBeGreaterThan(0);

      const renderCalls = graphqlResponses.filter(
        (r) => r.body.includes('renderHtml') || r.body.includes('Report__')
      );

      for (const r of graphqlResponses) {
        expect(r.status, `GraphQL should return 200`).toBe(200);
      }

      if (renderCalls.length > 0) {
        const htmlResponse = renderCalls.find((r) => r.body.includes('<') || r.body.length > 100);
        expect(htmlResponse, 'renderHtml response should be non-empty').toBeTruthy();
      }
    });
  });
}

export interface ReportValueAssertion {
  reportLabel: string;
  route: string;
  query: string;
  variables: Record<string, unknown>;
  responseKey: string;
  expectedTokens: string[];
}

export function assertReportRenderedWithValue(cfg: ReportValueAssertion): void {
  test.describe(`${cfg.reportLabel} report render values`, () => {
    test('renderHtml returns HTML containing deterministic seed values', async ({ page }) => {
      await loginAndNavigate(page, cfg.route);

      const resp = await page.request.post('/graphql', {
        data: { query: cfg.query, variables: cfg.variables },
      });
      expect(resp.status(), 'renderHtml GraphQL should return 200').toBe(200);

      const json = await resp.json();
      const html: string = json?.data?.[cfg.responseKey] ?? '';
      expect(html.length, 'rendered HTML should be non-empty').toBeGreaterThan(0);

      const normalized = html.replace(/,/g, '');
      for (const token of cfg.expectedTokens) {
        const normToken = token.replace(/,/g, '');
        expect(
          normalized.includes(normToken),
          `rendered HTML should contain token "${token}"`,
        ).toBe(true);
      }
    });
  });
}

// ===================== Download path assertions =====================

const DOMAIN_BIZ_NAME: Record<string, string> = {
  fin: 'ErpFinReport',
  mfg: 'ErpMfgReport',
  ast: 'ErpAstReport',
  mnt: 'ErpMntReport',
  prj: 'ErpPrjReport',
  qa: 'ErpQaReport',
  md: 'ErpMdReport',
  inv: 'ErpInvReport',
  cs: 'ErpCsReport',
  crm: 'ErpCrmReport',
  hr: 'ErpHrReport',
};

const CJK_RADICAL_NORMALIZE: Record<string, string> = {
  '⼯': '工', '⼼': '心', '⽃': '斗', '⽐': '比',
  '⽣': '生', '⽬': '目', '⾦': '金', '⼊': '入',
};

function normalizeCjkRadicals(text: string): string {
  let out = '';
  for (const c of text) {
    out += CJK_RADICAL_NORMALIZE[c] || c;
  }
  return out;
}

function extractXlsxText(buf: Buffer): string {
  let eocdOff = -1;
  for (let i = buf.length - 22; i >= 0 && i >= buf.length - 100; i--) {
    if (buf[i] === 0x50 && buf[i + 1] === 0x4b && buf[i + 2] === 0x05 && buf[i + 3] === 0x06) {
      eocdOff = i;
      break;
    }
  }
  if (eocdOff < 0) return '';
  const cdOffset = buf.readInt32LE(eocdOff + 16);
  const cdEntries = buf.readInt16LE(eocdOff + 10);

  const parts: string[] = [];
  let off = cdOffset;
  for (let i = 0; i < cdEntries; i++) {
    if (off + 46 > buf.length) break;
    if (buf[off] !== 0x50 || buf[off + 1] !== 0x4b || buf[off + 2] !== 0x01 || buf[off + 3] !== 0x02) break;
    const compMethod = buf.readUInt16LE(off + 10);
    const compSize = buf.readUInt32LE(off + 20);
    const nameLen = buf.readUInt16LE(off + 28);
    const extraLen = buf.readUInt16LE(off + 30);
    const commentLen = buf.readUInt16LE(off + 32);
    const localHeaderOff = buf.readUInt32LE(off + 42);
    const name = buf.slice(off + 46, off + 46 + nameLen).toString('utf8');

    if ((name.startsWith('xl/worksheets/') && name.endsWith('.xml')) || name === 'xl/sharedStrings.xml') {
      if (localHeaderOff + 30 > buf.length) { off += 46 + nameLen + extraLen + commentLen; continue; }
      const lNameLen = buf.readUInt16LE(localHeaderOff + 26);
      const lExtraLen = buf.readUInt16LE(localHeaderOff + 28);
      const dataStart = localHeaderOff + 30 + lNameLen + lExtraLen;
      const dataEnd = dataStart + compSize;
      if (dataEnd > buf.length) { off += 46 + nameLen + extraLen + commentLen; continue; }
      const chunk = buf.slice(dataStart, dataEnd);
      let xml = '';
      if (compMethod === 0) {
        xml = chunk.toString('utf8');
      } else if (compMethod === 8) {
        try {
          xml = zlib.inflateRawSync(chunk).toString('utf8');
        } catch {
          xml = '';
        }
      }
      const re = /<t[^>]*>([^<]*)<\/t>/g;
      let m: RegExpExecArray | null;
      while ((m = re.exec(xml)) !== null) {
        parts.push(m[1]);
      }
    }
    off += 46 + nameLen + extraLen + commentLen;
  }
  return parts.join('');
}

function extractPdfText(buf: Buffer): string {
  const data = buf.toString('latin1');
  const streamBlocks: Buffer[] = [];
  const streamRe = /stream\r?\n/g;
  let m: RegExpExecArray | null;
  while ((m = streamRe.exec(data)) !== null) {
    const start = m.index + m[0].length;
    const endIdx = data.indexOf('endstream', start);
    if (endIdx < 0) break;
    const chunk = buf.slice(start, start + (endIdx - start));
    streamBlocks.push(chunk);
    streamRe.lastIndex = endIdx + 9;
  }
  let combined = '';
  for (const block of streamBlocks) {
    let decoded: string;
    try {
      decoded = zlib.inflateSync(block).toString('latin1');
    } catch {
      decoded = block.toString('latin1');
    }
    combined += '\n' + decoded;
  }
  const allBytes = Buffer.from(combined, 'latin1');
  const allDec = allBytes.toString('latin1');

  const gmap = new Map<number, number>();
  const bfrangeRe = /beginbfrange([\s\S]*?)endbfrange/g;
  let rm: RegExpExecArray | null;
  while ((rm = bfrangeRe.exec(allDec)) !== null) {
    const block = rm[1];
    const tripleRe = /<([0-9A-Fa-f]+)>\s*<([0-9A-Fa-f]+)>\s*<([0-9A-Fa-f]+)>/g;
    let tm: RegExpExecArray | null;
    while ((tm = tripleRe.exec(block)) !== null) {
      const s = parseInt(tm[1], 16);
      const e = parseInt(tm[2], 16);
      const base = parseInt(tm[3], 16);
      for (let i = s; i <= e; i++) gmap.set(i, base + (i - s));
    }
    const arrRe = /<([0-9A-Fa-f]+)>\s*<([0-9A-Fa-f]+)>\s*\[([^\]]+)\]/g;
    let am: RegExpExecArray | null;
    while ((am = arrRe.exec(block)) !== null) {
      const s = parseInt(am[1], 16);
      const e = parseInt(am[2], 16);
      const cps = am[3].match(/<([0-9A-Fa-f]+)>/g) || [];
      for (let idx = 0; idx < cps.length && s + idx <= e; idx++) {
        gmap.set(s + idx, parseInt(cps[idx].replace(/[<>]/g, ''), 16));
      }
    }
  }
  const bfcharRe = /beginbfchar([\s\S]*?)endbfchar/g;
  while ((rm = bfcharRe.exec(allDec)) !== null) {
    const pairRe = /<([0-9A-Fa-f]{1,4})>\s*<([0-9A-Fa-f]+)>/g;
    let pm: RegExpExecArray | null;
    while ((pm = pairRe.exec(rm[1])) !== null) {
      gmap.set(parseInt(pm[1], 16), parseInt(pm[2], 16));
    }
  }

  function decodeHexTj(hex: string): string {
    let out = '';
    for (let i = 0; i + 4 <= hex.length; i += 4) {
      const g = parseInt(hex.substr(i, 4), 16);
      const cp = gmap.get(g);
      if (cp) {
        try {
          out += String.fromCodePoint(cp);
        } catch {
          // skip
        }
      }
    }
    return out;
  }

  let text = '';
  const tjRe = /<([0-9A-Fa-f]+)>\s*Tj/g;
  while ((m = tjRe.exec(allDec)) !== null) {
    text += decodeHexTj(m[1]);
  }
  const tjArrRe = /\[([^\]]*)\]\s*TJ/g;
  while ((m = tjArrRe.exec(allDec)) !== null) {
    const arr = m[1];
    const hexRe = /<([0-9A-Fa-f]+)>/g;
    let hm: RegExpExecArray | null;
    while ((hm = hexRe.exec(arr)) !== null) {
      text += decodeHexTj(hm[1]);
    }
  }
  return normalizeCjkRadicals(text);
}

export interface ReportDownloadCase {
  domain: string;
  reportName: string;
  data: Record<string, unknown>;
  expectedTokens: string[];
}

export interface ReportDownloadAssertion {
  domain: string;
  reportName: string;
  renderType: 'xlsx' | 'pdf';
  data: Record<string, unknown>;
  expectedTokens: string[];
}

export function assertReportDownload(cfg: ReportDownloadAssertion): void {
  const bizName = DOMAIN_BIZ_NAME[cfg.domain];
  if (!bizName) throw new Error(`Unknown domain: ${cfg.domain}`);
  test.describe(`${cfg.domain} ${cfg.reportName} ${cfg.renderType} download`, () => {
    test('returns non-empty binary with correct magic + structure token', async ({ page }) => {
      await loginAndNavigate(page, `/${cfg.reportName}`);

      const resp = await page.request.post(`/p/${bizName}__download`, {
        headers: { 'Content-Type': 'application/json' },
        data: {
          reportName: cfg.reportName,
          renderType: cfg.renderType,
          data: cfg.data,
        },
      });

      expect(
        resp.status(),
        `${cfg.reportName} ${cfg.renderType} download should return 200`,
      ).toBe(200);

      const body = await resp.body();
      expect(
        body.length,
        `${cfg.reportName} ${cfg.renderType} download body should be non-empty`,
      ).toBeGreaterThan(0);

      const magic = body.slice(0, 4).toString('latin1');
      if (cfg.renderType === 'xlsx') {
        expect(
          magic,
          `${cfg.reportName} xlsx should start with PK\\x03\\x04 magic`,
        ).toBe('PK\x03\x04');
      } else {
        expect(
          magic,
          `${cfg.reportName} pdf should start with %PDF magic`,
        ).toBe('%PDF');
      }

      const text = cfg.renderType === 'xlsx'
        ? normalizeCjkRadicals(extractXlsxText(body))
        : extractPdfText(body);
      const matched = cfg.expectedTokens.filter((t) => text.includes(t));
      expect(
        matched.length > 0,
        `${cfg.reportName} ${cfg.renderType} should contain at least one expected token (checked: ${cfg.expectedTokens.join(', ')})`,
      ).toBe(true);
    });
  });
}

// ===================== AMIS download button assertions =====================
//
// Distinct from `assertReportDownload` (which calls `/p/` directly via
// `page.request.post`, bypassing AMIS), this layer drives the REAL AMIS
// download button declared in each report's page.yaml. It proves the
// page.yaml `api` wiring (url `/p/{biz}__download` + REST body map) actually
// triggers a browser file download when the user clicks "下载 XLSX/PDF".
//
// Proof model: the AMIS button's `actionType: download` issues an HTTP request
// (read as a blob) to its configured `api.url`, then triggers a file save via
// a blob <a download>. We capture BOTH:
//   1. the `/p/{biz}__download` REQUEST (proves the api wiring sends the
//      request to the right endpoint — catches a url regression back to
//      `/graphql`, which would never fire a binary download). Request-level
//      capture is used because `response.body()` returns empty for responses
//      the page consumes as a blob (the stream is transferred to the Blob);
//   2. the browser `download` event → the actually-saved file (proves the
//      user-facing file save fires AND gives us the real bytes for magic +
//      token assertions — identical rigor to the direct `/p/` layer).

export interface AmisDownloadAssertion {
  domain: string;
  reportName: string;
  renderType: 'xlsx' | 'pdf';
  route: string;
  /** Form field values to fill before clicking download (input-number/input-text
   * fields with a fillable <input name>). Date params have no fillable name and
   * are left empty (backend treats null date ranges as full extent). */
  formFields?: Record<string, string>;
  /** AMIS input-date fields to fill, keyed by form-item label text. Date inputs
   * have no fillable <input name>, so they are targeted by the label of their
   * enclosing .cxd-Form-item wrapper. Needed for date-param reports: the backend
   * strict-parses the date string and an AMIS empty string ("") throws
   * DateTimeParseException, so a concrete seed date must be supplied to exercise
   * the download pipeline (mirrors the renderHtml visual helper's fillDates). */
  fillDates?: Record<string, string>;
  expectedTokens: string[];
}

export function assertAmisDownloadButton(cfg: AmisDownloadAssertion): void {
  const bizName = DOMAIN_BIZ_NAME[cfg.domain];
  if (!bizName) throw new Error(`Unknown domain: ${cfg.domain}`);
  const downloadUrl = `/p/${bizName}__download`;
  test.describe(`${cfg.domain} ${cfg.reportName} ${cfg.renderType} AMIS download button`, () => {
    test('AMIS download button triggers binary download via /p/ endpoint', async ({ page }) => {
      await loginAndNavigate(page, cfg.route);

      if (cfg.formFields) {
        for (const [name, value] of Object.entries(cfg.formFields)) {
          await page.locator(`input[name="${name}"]`).first().fill(value);
        }
      }

      if (cfg.fillDates) {
        for (const [label, value] of Object.entries(cfg.fillDates)) {
          await page.locator('.cxd-Form-item').filter({ hasText: label }).locator('input').first().fill(value);
        }
      }

      const label = cfg.renderType === 'xlsx' ? /下载\s*XLSX/ : /下载\s*PDF/;

      // Request-level capture proves the button targeted the /p/ binary-download
      // endpoint (catches a url regression back to /graphql, which would never
      // resolve this promise — no /p/ request would be issued).
      const requestPromise = page.waitForRequest(
        (req) => req.url().includes(downloadUrl) && req.method() === 'POST',
        { timeout: 30_000 },
      );
      const downloadPromise = page.waitForEvent('download', { timeout: 30_000 });

      await page.getByRole('button', { name: label }).first().click();

      const req = await requestPromise;
      expect(
        req.url(),
        `${cfg.reportName} ${cfg.renderType} AMIS button should POST to ${downloadUrl}`,
      ).toContain(downloadUrl);

      // The download event fires when AMIS converts the blob response into a
      // file save. The saved file gives us the real bytes for magic + token
      // assertions (response.body() is empty for blob-consumed responses).
      const dl = await downloadPromise;
      expect(
        dl.suggestedFilename(),
        `${cfg.reportName} ${cfg.renderType} download filename should reflect renderType`,
      ).toMatch(new RegExp(cfg.renderType, 'i'));

      let body: Buffer;
      const savedPath = await dl.path();
      if (savedPath && fs.existsSync(savedPath)) {
        body = fs.readFileSync(savedPath);
      } else {
        body = await dl.path().then(() => Buffer.from([]));
        const tmp = `/tmp/amis-dl-${cfg.reportName}-${cfg.renderType}`;
        await dl.saveAs(tmp);
        body = fs.readFileSync(tmp);
      }

      expect(
        body.length,
        `${cfg.reportName} ${cfg.renderType} AMIS download file should be non-empty`,
      ).toBeGreaterThan(0);

      const magic = body.slice(0, 4).toString('latin1');
      if (cfg.renderType === 'xlsx') {
        expect(
          magic,
          `${cfg.reportName} xlsx should start with PK\\x03\\x04 magic`,
        ).toBe('PK\x03\x04');
      } else {
        expect(
          magic,
          `${cfg.reportName} pdf should start with %PDF magic`,
        ).toBe('%PDF');
      }

      const text = cfg.renderType === 'xlsx'
        ? normalizeCjkRadicals(extractXlsxText(body))
        : extractPdfText(body);
      const matched = cfg.expectedTokens.filter((t) => text.includes(t));
      expect(
        matched.length > 0,
        `${cfg.reportName} ${cfg.renderType} AMIS download should contain at least one expected token (checked: ${cfg.expectedTokens.join(', ')})`,
      ).toBe(true);
    });
  });
}
