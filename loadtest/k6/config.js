import { check } from "k6";

export const BASE_URL = __ENV.BASE_URL || "http://localhost:8080/api/v1";
export const AUTHOR_EMAIL = __ENV.AUTHOR_EMAIL || "loadtest.author@example.com";
export const COMMENTER_EMAIL =
  __ENV.COMMENTER_EMAIL || "loadtest.commenter@example.com";
export const PASSWORD = __ENV.LOADTEST_PASSWORD || "password123!";
export const POST_TITLE_PREFIX =
  __ENV.LOADTEST_POST_TITLE_PREFIX || "[loadtest] post ";

export function defaultOptions(name) {
  return {
    vus: Number(__ENV.VUS || 10),
    duration: __ENV.DURATION || "30s",
    thresholds: {
      http_req_failed: ["rate<0.01"],
      [`http_req_duration{scenario:${name}}`]: ["p(95)<3000"],
    },
  };
}

export function assertSuccess(response, name) {
  check(response, {
    [`${name} status is 200`]: (res) => res.status === 200,
  });
}
