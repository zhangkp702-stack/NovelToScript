<script setup>
const chapters = defineModel({ type: Array, default: () => ["", "", ""] });

function addChapter() {
  chapters.value.push("");
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
        placeholder="请输入章节内容"
      />
    </div>
    <button type="button" class="add-chapter-btn" @click="addChapter">添加章节</button>
  </div>
</template>
