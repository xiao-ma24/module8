import { computed, ref } from 'vue'

const ROLE_KEY = 'demo_role'

/** 演示用角色：admin 系统管理员 / teacher 普通教师 */
const role = ref(localStorage.getItem(ROLE_KEY) === 'teacher' ? 'teacher' : 'admin')

const roleOptions = [
  { label: '系统管理员', value: 'admin' },
  { label: '普通教师', value: 'teacher' }
]

const roleName = computed(() => (role.value === 'admin' ? '系统管理员' : '普通教师'))
const isAdmin = computed(() => role.value === 'admin')

function setRole(value) {
  role.value = value === 'teacher' ? 'teacher' : 'admin'
  localStorage.setItem(ROLE_KEY, role.value)
}

export function getRoleValue() {
  return role.value
}

export function useRole() {
  return { role, roleName, roleOptions, isAdmin, setRole }
}
