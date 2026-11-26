export class CatalogueProvider {
  async listBatches() { throw new Error("CatalogueProvider.listBatches not implemented"); }
  async listCourses(batchId) { throw new Error("CatalogueProvider.listCourses not implemented"); }
  async getCourse(courseId) { throw new Error("CatalogueProvider.getCourse not implemented"); }
  async listChapters(courseId) { throw new Error("CatalogueProvider.listChapters not implemented"); }
  async listLectures(chapterId, type) { throw new Error("CatalogueProvider.listLectures not implemented"); }
  async listLiveClasses() { throw new Error("CatalogueProvider.listLiveClasses not implemented"); }
}
export class IdentityProvider {
  async register(input) { throw new Error("IdentityProvider.register not implemented"); }
  async authenticate(input) { throw new Error("IdentityProvider.authenticate not implemented"); }
}
export class EntitlementProvider {
  async check(userId, contentId) { throw new Error("EntitlementProvider.check not implemented"); }
}
export class PlaybackProvider {
  async createSession(userId, contentId) { throw new Error("PlaybackProvider.createSession not implemented"); }
  async refreshSession(userId, sessionId) { throw new Error("PlaybackProvider.refreshSession not implemented"); }
  async closeSession(userId, sessionId) { throw new Error("PlaybackProvider.closeSession not implemented"); }
}

// RWA-specific contracts are deliberately transport-agnostic. No endpoint,
// credential, request schema, or Original RWA response format is assumed.
export class RwaIdentityProvider {
  async authenticate(input) { throw new Error("RwaIdentityProvider.authenticate not implemented"); }
  async refreshSession(userId) { throw new Error("RwaIdentityProvider.refreshSession not implemented"); }
  async validateSession(userId) { throw new Error("RwaIdentityProvider.validateSession not implemented"); }
  async revokeSession(userId) { throw new Error("RwaIdentityProvider.revokeSession not implemented"); }
}
export class RwaCatalogueProvider {
  async listBatches() { throw new Error("RwaCatalogueProvider.listBatches not implemented"); }
  async listCourses(batchId) { throw new Error("RwaCatalogueProvider.listCourses not implemented"); }
  async getCourse(courseId) { throw new Error("RwaCatalogueProvider.getCourse not implemented"); }
  async listChapters(courseId) { throw new Error("RwaCatalogueProvider.listChapters not implemented"); }
  async listLectures(chapterId, type) { throw new Error("RwaCatalogueProvider.listLectures not implemented"); }
  async listLiveClasses() { throw new Error("RwaCatalogueProvider.listLiveClasses not implemented"); }
}
export class RwaEntitlementProvider {
  async checkAccountAccess(userId) { throw new Error("RwaEntitlementProvider.checkAccountAccess not implemented"); }
  async checkCourseAccess(userId, courseId) { throw new Error("RwaEntitlementProvider.checkCourseAccess not implemented"); }
  async checkLectureAccess(userId, lectureId) { throw new Error("RwaEntitlementProvider.checkLectureAccess not implemented"); }
  async getSubscriptionStatus(userId) { throw new Error("RwaEntitlementProvider.getSubscriptionStatus not implemented"); }
}
export class RwaPlaybackProvider {
  async getLectureDetails(contentId) { throw new Error("RwaPlaybackProvider.getLectureDetails not implemented"); }
  async createSession(userId, contentId) { throw new Error("RwaPlaybackProvider.createSession not implemented"); }
  async refreshSession(userId, contentId) { throw new Error("RwaPlaybackProvider.refreshSession not implemented"); }
  async closeSession(userId, contentId) { throw new Error("RwaPlaybackProvider.closeSession not implemented"); }
}
export class LiveJoinProvider {
  async getLiveClassDetails(liveClassId) { throw new Error("LiveJoinProvider.getLiveClassDetails not implemented"); }
  async createJoinSession(userId, liveClassId) { throw new Error("LiveJoinProvider.createJoinSession not implemented"); }
  async refreshJoinSession(userId, liveClassId) { throw new Error("LiveJoinProvider.refreshJoinSession not implemented"); }
  async leaveSession(userId, liveClassId) { throw new Error("LiveJoinProvider.leaveSession not implemented"); }
}
