import { AUTHOR_EMAIL, COMMENTER_EMAIL, defaultOptions } from "../config.js";
import { login } from "../lib/auth.js";
import { createComment } from "../lib/comment.js";
import { loadLoadtestPostIds } from "../lib/post.js";

const SCENARIO = "comment-notification-delay-300ms";

export const options = defaultOptions(SCENARIO);

export function setup() {
  return {
    authorToken: login(AUTHOR_EMAIL),
    commenterToken: login(COMMENTER_EMAIL),
    postIds: loadLoadtestPostIds(),
  };
}

export default function (data) {
  const postId = data.postIds[__ITER % data.postIds.length];
  createComment(
    postId,
    data.commenterToken,
    `[${SCENARIO}] comment ${__VU}-${__ITER}`,
    SCENARIO,
  );
}
