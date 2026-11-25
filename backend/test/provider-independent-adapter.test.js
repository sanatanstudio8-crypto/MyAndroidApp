import test from "node:test";
import assert from "node:assert/strict";
import { ProviderIndependentRwaAdapter } from "../src/providers/rwa/provider-independent-adapter.js";
import { ProviderNotConfiguredError } from "../src/providers/errors.js";

test("adapter reports missing official contracts without guessing provider schemas", async () => {
  const adapter = new ProviderIndependentRwaAdapter();
  assert.equal(adapter.configurationStatus.ready, false);
  assert.ok(adapter.configurationStatus.missingOperations.includes("authenticate"));
  await assert.rejects(() => adapter.authenticate({}), ProviderNotConfiguredError);
  await assert.rejects(() => adapter.catalogue.listBatches(), ProviderNotConfiguredError);
  await assert.rejects(() => adapter.entitlement.checkLectureAccess("u", "l"), ProviderNotConfiguredError);
  await assert.rejects(() => adapter.playback.createSession("u", "l"), ProviderNotConfiguredError);
  await assert.rejects(() => adapter.liveJoin.createJoinSession("u", "live"), ProviderNotConfiguredError);
});

test("adapter normalizes provider-supplied catalogue, entitlement and sessions", async () => {
  const operations = {
    authenticate: async () => ({ account: "raw" }),
    listBatches: async () => ({ records: [{ key: "b1", label: "Batch 1" }] }),
    checkLectureAccess: async () => ({ access: "raw" }),
    createPlaybackSession: async () => ({ session: "raw" }),
    createLiveJoinSession: async () => ({ room: "raw" })
  };
  const normalizers = {
    accountSession: () => ({ accountId: "acct", userId: "user", status: "authenticated" }),
    batches: raw => raw.records.map(x => ({ id: x.key, title: x.label })),
    entitlement: () => ({ contentId: "lecture-1", decision: "granted" }),
    playbackSession: () => ({ contentId: "lecture-1", status: "created", playbackUrl: "https://provider.example.test/session" }),
    liveJoinSession: () => ({ liveClassId: "live-1", status: "created", joinUrl: "https://provider.example.test/join" })
  };
  const adapter = new ProviderIndependentRwaAdapter({ operations, normalizers });
  assert.equal((await adapter.authenticate({})).accountId, "acct");
  assert.equal((await adapter.catalogue.listBatches())[0].id, "b1");
  assert.equal((await adapter.entitlement.checkLectureAccess("user", "lecture-1")).decision, "granted");
  assert.equal((await adapter.playback.createSession("user", "lecture-1")).status, "created");
  assert.equal((await adapter.liveJoin.createJoinSession("user", "live-1")).status, "created");
});
