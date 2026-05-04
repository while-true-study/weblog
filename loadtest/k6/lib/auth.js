import http from "k6/http";
import { BASE_URL, PASSWORD, assertSuccess } from "../config.js";

export function login(email) {
  const response = http.post(
    `${BASE_URL}/auth/login`,
    JSON.stringify({
      email,
      password: PASSWORD,
    }),
    {
      headers: { "Content-Type": "application/json" },
      tags: { endpoint: "auth-login" },
    },
  );

  assertSuccess(response, "login");

  const body = response.json();
  if (!body?.success || !body?.data?.accessToken) {
    throw new Error(`Login failed for ${email}: ${response.body}`);
  }

  return body.data.accessToken;
}
