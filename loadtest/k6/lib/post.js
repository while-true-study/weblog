import http from "k6/http";
import { BASE_URL, POST_TITLE_PREFIX, assertSuccess } from "../config.js";

export function loadLoadtestPostIds() {
  const response = http.get(
    `${BASE_URL}/posts?page=0&size=50&sort=createdAt,DESC`,
    {
      tags: { endpoint: "post-list" },
    },
  );

  assertSuccess(response, "load posts");

  const body = response.json();
  const content = body?.content || [];
  const postIds = content
    .filter((post) => typeof post.title === "string" && post.title.startsWith(POST_TITLE_PREFIX))
    .map((post) => post.id);

  if (postIds.length === 0) {
    throw new Error("No loadtest posts found. Start the app with loadtest profile bootstrap.");
  }

  return postIds;
}

export function updatePost(postId, token, index, scenario) {
  const response = http.patch(
    `${BASE_URL}/posts/${postId}`,
    JSON.stringify({
      title: `${POST_TITLE_PREFIX}${String(index).padStart(2, "0")} updated ${Date.now()}`,
    }),
    {
      headers: {
        "Content-Type": "application/json",
        Authorization: `Bearer ${token}`,
      },
      tags: {
        endpoint: "post-update",
        scenario,
      },
    },
  );

  assertSuccess(response, "update post");
  return response;
}
