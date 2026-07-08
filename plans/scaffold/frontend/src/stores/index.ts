import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useAppStore = defineStore('app', () => {
  const sidebarCollapsed = ref(false)
  const currentAgent = ref<any>(null)

  function toggleSidebar() {
    sidebarCollapsed.value = !sidebarCollapsed.value
  }

  function setCurrentAgent(agent: any) {
    currentAgent.value = agent
  }

  return {
    sidebarCollapsed,
    currentAgent,
    toggleSidebar,
    setCurrentAgent
  }
})