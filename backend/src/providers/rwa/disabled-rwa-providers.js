import { RwaIdentityProvider, RwaCatalogueProvider, RwaEntitlementProvider, RwaPlaybackProvider, LiveJoinProvider } from "../contracts.js";
import { ProviderNotConfiguredError } from "../errors.js";

// Safe default: all real Original RWA transports remain unavailable until
// official documentation, explicit authorization, and a sandbox are supplied.
const unavailable = () => { throw new ProviderNotConfiguredError("original-rwa"); };
export class DisabledRwaIdentityProvider extends RwaIdentityProvider {
  async authenticate() { return unavailable(); }
  async refreshSession() { return unavailable(); }
  async validateSession() { return unavailable(); }
  async revokeSession() { return unavailable(); }
}
export class DisabledRwaCatalogueProvider extends RwaCatalogueProvider {
  async listBatches() { return unavailable(); }
  async listCourses() { return unavailable(); }
  async getCourse() { return unavailable(); }
  async listChapters() { return unavailable(); }
  async listLectures() { return unavailable(); }
  async listLiveClasses() { return unavailable(); }
}
export class DisabledRwaEntitlementProvider extends RwaEntitlementProvider {
  async checkAccountAccess() { return unavailable(); }
  async checkCourseAccess() { return unavailable(); }
  async checkLectureAccess() { return unavailable(); }
  async getSubscriptionStatus() { return unavailable(); }
}
export class DisabledRwaPlaybackProvider extends RwaPlaybackProvider {
  async getLectureDetails() { return unavailable(); }
  async createSession() { return unavailable(); }
  async refreshSession() { return unavailable(); }
  async closeSession() { return unavailable(); }
}
export class DisabledLiveJoinProvider extends LiveJoinProvider {
  async getLiveClassDetails() { return unavailable(); }
  async createJoinSession() { return unavailable(); }
  async refreshJoinSession() { return unavailable(); }
  async leaveSession() { return unavailable(); }
}
export function createDisabledRwaProviders() {
  return Object.freeze({
    enabled: false,
    identity: new DisabledRwaIdentityProvider(),
    catalogue: new DisabledRwaCatalogueProvider(),
    entitlement: new DisabledRwaEntitlementProvider(),
    playback: new DisabledRwaPlaybackProvider(),
    liveJoin: new DisabledLiveJoinProvider()
  });
}
