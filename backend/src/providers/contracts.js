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
}
