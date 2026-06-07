export function formatScriptPreview(script) {
  if (!script || typeof script !== "object") {
    return "";
  }

  const lines = [];
  const title = script.metadata?.title;
  if (title) {
    lines.push(`标题：${title}`);
    lines.push("");
  }

  const characterNames = buildCharacterNameMap(script.characters);
  const scenes = Array.isArray(script.scenes) ? script.scenes : [];
  for (const scene of scenes) {
    const sceneTitle = scene.scene_title || scene.scene_id || "场景";
    const location = scene.location ? ` @ ${scene.location}` : "";
    const time = scene.time ? `（${scene.time}）` : "";
    lines.push(`【${sceneTitle}】${location}${time}`);
    if (scene.action) {
      lines.push(`动作：${scene.action}`);
    }
    const dialogues = Array.isArray(scene.dialogues) ? scene.dialogues : [];
    for (const dialogue of dialogues) {
      const speaker = resolveSpeaker(dialogue.speaker, characterNames);
      lines.push(`${speaker}：${dialogue.content || ""}`);
    }
    lines.push("");
  }

  if (lines.length === 0) {
    return JSON.stringify(script, null, 2);
  }
  return lines.join("\n").trim();
}

function buildCharacterNameMap(characters) {
  const map = new Map();
  if (!Array.isArray(characters)) {
    return map;
  }
  for (const character of characters) {
    if (character?.id && character?.name) {
      map.set(character.id, character.name);
    }
  }
  return map;
}

function resolveSpeaker(speaker, characterNames) {
  if (!speaker) {
    return "旁白";
  }
  return characterNames.get(speaker) || speaker;
}
