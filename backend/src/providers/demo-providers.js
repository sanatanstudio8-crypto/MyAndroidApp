import { LiveJoinProvider } from "./contracts.js";

const expiresAt = minutes => new Date(Date.now() + minutes * 60_000).toISOString();

export const DEMO_CATALOGUE = Object.freeze({
  batches: [
    { id: "demo-free-batch", title: "DEMO — Free Foundation Batch", name: "DEMO — Free Foundation Batch" },
    { id: "demo-premium-batch", title: "DEMO — Premium Practice Batch", name: "DEMO — Premium Practice Batch" }
  ],
  courses: [
    { id: "demo-free-course", batchId: "demo-free-batch", title: "DEMO — General Studies Basics", description: "Sample Free course.", visibility: "free" },
    { id: "demo-premium-course", batchId: "demo-premium-batch", title: "DEMO — Advanced Exam Preparation", description: "Sample Premium course.", visibility: "paid" }
  ],
  chapters: [
    { id: "demo-free-chapter", courseId: "demo-free-course", title: "DEMO — Introduction", position: 1 },
    { id: "demo-premium-chapter", courseId: "demo-premium-course", title: "DEMO — Advanced Concepts", position: 1 }
  ],
  lectures: [
    { id: "demo-free-lecture", chapterId: "demo-free-chapter", title: "DEMO — Free Sample Lesson", description: "A locally bundled sample video.", type: "recorded", visibility: "free" },
    { id: "demo-premium-lecture", chapterId: "demo-premium-chapter", title: "DEMO — Premium Sample Lesson", description: "A locally bundled Premium sample video.", type: "recorded", visibility: "paid" }
  ],
  liveClasses: [
    { id: "demo-free-live-class", title: "DEMO — Free Live Room", courseId: "demo-free-course", status: "available", visibility: "free" },
    { id: "demo-premium-live-class", title: "DEMO — Premium Live Room", courseId: "demo-premium-course", status: "available", visibility: "paid" }
  ]
});

const DEMO_DECISIONS = new Map([
  ["demo-free-lecture", "granted"],
  ["demo-premium-lecture", "granted"],
  ["demo-free-live-class", "granted"],
  ["demo-premium-live-class", "granted"],
  ["demo-denied-lecture", "denied"],
  ["demo-denied-live-class", "denied"],
  ["demo-expired-lecture", "expired"],
  ["demo-expired-live-class", "expired"]
]);

export class DemoEntitlementProvider {
  async check(_userId, contentId) {
    const decision = DEMO_DECISIONS.get(contentId) || "unknown";
    return Object.freeze({
      contentId,
      decision,
      source: "demo_only",
      checkedAt: new Date().toISOString(),
      expiresAt: decision === "expired" ? new Date(Date.now() - 60_000).toISOString() : expiresAt(15)
    });
  }
}

export class DemoPlaybackProvider {
  constructor({ entitlementProvider = new DemoEntitlementProvider() } = {}) {
    this.entitlementProvider = entitlementProvider;
    this.sessions = new Map();
  }

  async createSession(userId, contentId) {
    const entitlement = await this.entitlementProvider.check(userId, contentId);
    if (entitlement.decision !== "granted") return { contentId, status: entitlement.decision, entitlement, mediaRef: null };
    const mediaRef = contentId === "demo-free-lecture" ? "demo-free" :
      contentId === "demo-premium-lecture" ? "demo-premium" : null;
    if (!mediaRef) return { contentId, status: "unknown", entitlement, mediaRef: null };
    const session = Object.freeze({
      id: `demo-playback:${userId}:${contentId}`,
      contentId,
      status: "demo_preview",
      mediaRef,
      expiresAt: expiresAt(15),
      source: "demo_only",
      providerAuthorization: false
    });
    this.sessions.set(`${userId}:${contentId}`, session);
    return session;
  }

  async closeSession(userId, contentId) {
    return this.sessions.delete(`${userId}:${contentId}`);
  }
}

// This is intentionally independent from recorded playback. It creates only
// in-process demo-room membership and never returns a meeting or provider URL.
export class DemoLiveJoinProvider extends LiveJoinProvider {
  constructor({ entitlementProvider = new DemoEntitlementProvider() } = {}) {
    super();
    this.entitlementProvider = entitlementProvider;
    this.sessions = new Map();
  }

  async getLiveClassDetails(liveClassId) {
    const room = DEMO_CATALOGUE.liveClasses.find(item => item.id === liveClassId);
    return room ? { ...room, roomType: "local_demo" } : null;
  }

  async createJoinSession(userId, liveClassId) {
    const entitlement = await this.entitlementProvider.check(userId, liveClassId);
    if (entitlement.decision !== "granted") return { liveClassId, status: entitlement.decision, entitlement, roomType: null };
    const room = await this.getLiveClassDetails(liveClassId);
    if (!room) return { liveClassId, status: "unknown", entitlement, roomType: null };
    const session = Object.freeze({
      id: `demo-live:${userId}:${liveClassId}`,
      liveClassId,
      title: room.title,
      status: "joined",
      roomType: "local_demo",
      joinedAt: new Date().toISOString(),
      expiresAt: expiresAt(30),
      source: "demo_only",
      providerAuthorization: false
    });
    this.sessions.set(`${userId}:${liveClassId}`, session);
    return session;
  }

  async refreshJoinSession(userId, liveClassId) {
    return this.sessions.get(`${userId}:${liveClassId}`) || null;
  }

  async leaveSession(userId, liveClassId) {
    return this.sessions.delete(`${userId}:${liveClassId}`);
  }
}
