import http from "k6/http";
import { BASE_URL, assertSuccess } from "../config.js";

export function createComment(postId, token, content, scenario) {
  const response = http.post(
    `${BASE_URL}/posts/${postId}/comments`,
    JSON.stringify({ content }),
    {
      headers: {
        "Content-Type": "application/json",
        Authorization: `Bearer ${token}`,
      },
      tags: {
        endpoint: "comment-create",
        scenario,
      },
    },
  );

  assertSuccess(response, "create comment");
  return response;
}
