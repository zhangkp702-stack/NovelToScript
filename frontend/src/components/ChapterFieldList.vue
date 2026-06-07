<script setup>
const chapters = defineModel({ type: Array, default: () => [""] });

defineProps({
  streamingIndexes: {
    type: Set,
    default: () => new Set()
  }
});

const emit = defineEmits(["generate"]);

function addChapter() {
  chapters.value.push("");
}

function onGenerate(index) {
  emit("generate", index);
}
</script>

<template>
  <div class="chapter-list">
    <div v-for="(_, index) in chapters" :key="index" class="chapter-item">
      <label :for="`chapter-${index}`">第 {{ index + 1 }} 章</label>
      <textarea
        :id="`chapter-${index}`"
        v-model="chapters[index]"
        rows="5"
        placeholder="请输入本章小说内容"
      />
      <button
        type="button"
        class="chapter-generate-btn"
        :disabled="streamingIndexes.has(index)"
        @click="onGenerate(index)"
      >
        {{ streamingIndexes.has(index) ? "生成中..." : "生成本章剧本" }}
      </button>
    </div>
    <button type="button" class="add-chapter-btn" @click="addChapter">添加章节</button>
  </div>
</template>
