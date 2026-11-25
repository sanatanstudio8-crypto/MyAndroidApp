import { RwaIdentityProvider, RwaCatalogueProvider, RwaEntitlementProvider, RwaPlaybackProvider, LiveJoinProvider } from "../contracts.js";
import { ProviderNotConfiguredError } from "../errors.js";

// Safe default: all real Original RWA transports remain unavailable until
// official documentation, explicit authorization, and a sandbox are supplied.
const unavailable = () => { throw new ProviderNotConfiguredError("original-rwa"); };
export class DisabledRwaIdentityProvider extends RwaIdentityProvider {
  authenticate() { return unavailable(); }
  refreshSession() { return unavailable(); }
  validateSession() { return unavailable(); }
  revokeSession() { return unavailable(); }
}
export class DisabledRwaCatalogueProvider extends RwaCatalogueProvider {
  listBatches() { return unavailable(); }
  listCourses() { return unavailable(); }
  getCourse() { return unavailable(); }
  listChapters() { return unavailable(); }
  listLectures() { return unavailable(); }
  listLiveClasses() { return unavailable(); }
}
export class DisabledRwaEntitlementProvider extends RwaEntitlementProvider {
  checkAccountAccess() { return unavailable(); }
  checkCourseAccess() { return unavailable(); }
  checkLectureAccess() { return unavailable(); }
  getSubscriptionStatus() { return unavailable(); }
}
export class DisabledRwaPlaybackProvider extends RwaPlaybackProvider {
  getLectureDetails() { return unavailable(); }
  createSession() { return unavailable(); }
  refreshSession() { return unavailable(); }
  closeSession() { return unavailable(); }
}
export class DisabledLiveJoinProvider extends LiveJoinProvider {
  getLiveClassDetails() { return unavailable(); }
  createJoinSession() { return unavailable(); }
  refreshJoinSession() { return unavailable(); }
  leaveSession() { return unavailable(); }
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
