import { describe, it, expect } from 'vitest';
import { upgradeNdbx, CURRENT_FORMAT_VERSION } from '../../src/ndbx/upgrades.js';

describe('NDBX Upgrades', () => {
  it('upgrades formatVersion', () => {
    const xml = '<ndbx formatVersion="1" type="file"><node name="root"/></ndbx>';
    const result = upgradeNdbx(xml, 1);
    expect(result.xml).toContain(`formatVersion="${CURRENT_FORMAT_VERSION}"`);
  });

  it('upgrades list.filter to list.cull', () => {
    const xml = '<ndbx formatVersion="6"><node name="root"><node name="f1" prototype="list.filter"/></node></ndbx>';
    const result = upgradeNdbx(xml, 6);
    expect(result.xml).toContain('prototype="list.cull"');
    expect(result.xml).not.toContain('prototype="list.filter"');
  });

  it('passes through current version unchanged', () => {
    const xml = `<ndbx formatVersion="${CURRENT_FORMAT_VERSION}"><node name="root"/></ndbx>`;
    const result = upgradeNdbx(xml, CURRENT_FORMAT_VERSION);
    expect(result.xml).toBe(xml);
  });

  it('CURRENT_FORMAT_VERSION is 21', () => {
    expect(CURRENT_FORMAT_VERSION).toBe(21);
  });
});
