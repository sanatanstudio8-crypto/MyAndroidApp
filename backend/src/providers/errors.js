export class ProviderError extends Error {
  constructor(message, { code = "PROVIDER_ERROR", provider = "rwa", retryable = false, cause } = {}) {
    super(message, cause ? { cause } : undefined);
    this.name = "ProviderError";
    this.code = code;
    this.provider = provider;
    this.retryable = Boolean(retryable);
  }
}
export class ProviderNotConfiguredError extends ProviderError {
  constructor(provider = "rwa") {
    super("Provider integration is not configured.", { code: "PROVIDER_NOT_CONFIGURED", provider });
    this.name = "ProviderNotConfiguredError";
  }
}
export class ProviderAuthenticationError extends ProviderError {
  constructor(message = "Provider authentication failed.", options = {}) {
    super(message, { ...options, code: "PROVIDER_AUTHENTICATION_FAILED" });
    this.name = "ProviderAuthenticationError";
  }
}
export class ProviderAuthorizationError extends ProviderError {
  constructor(message = "Provider authorization was not granted.", options = {}) {
    super(message, { ...options, code: "PROVIDER_AUTHORIZATION_DENIED" });
    this.name = "ProviderAuthorizationError";
  }
}
export class ProviderContractError extends ProviderError {
  constructor(message = "Provider response did not match the normalized contract.", options = {}) {
    super(message, { ...options, code: "PROVIDER_CONTRACT_INVALID" });
    this.name = "ProviderContractError";
  }
}
