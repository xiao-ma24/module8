import { createRouter, createWebHistory } from 'vue-router'
import Layout from '@/layout/index.vue'
import PlaceholderPage from '@/views/common/PlaceholderPage.vue'
import { getRoleValue } from '@/composables/useRole'

const roleHome = () => (getRoleValue() === 'teacher' ? '/base/purchase' : '/base/material')

const routes = [
  {
    path: '/',
    component: Layout,
    redirect: roleHome,
    children: [
      {
        path: 'overview',
        name: 'Overview',
        component: PlaceholderPage,
        meta: {
          title: '数据总览',
          desc: '全校物资存量、预警与流转数据的汇总视图，当前仅占位。'
        }
      },
      {
        path: 'base/material',
        name: 'MaterialInfo',
        component: () => import('@/views/base/material/index.vue'),
        meta: {
          title: '物资档案',
          group: '基础数据',
          groupRoute: '/base/material',
          desc: '维护物资档案，包括编码、规格、单位、参考单价与安全库存区间。'
        }
      },
      {
        path: 'base/catalog',
        name: 'MaterialCatalog',
        component: () => import('@/views/base/material/index.vue'),
        meta: {
          title: '物资目录',
          readonly: true,
          desc: '教师查看物资档案，仅供浏览。'
        }
      },
      {
        path: 'base/purchase',
        name: 'PersonalInvoice',
        component: () => import('@/views/base/purchase/index.vue'),
        meta: {
          title: '个人发票登记',
          group: '审批中心',
          groupRoute: '/base/purchase',
          desc: '教师个人科研经费发票的散单快速入库登记。'
        }
      },
      {
        path: 'base/approval',
        name: 'ApprovalCenter',
        component: () => import('@/views/base/approval/index.vue'),
        meta: {
          title: '审批中心',
          group: '审批中心',
          groupRoute: '/base/approval',
          desc: '库存变动申请的审核与记录查询。'
        }
      },
      {
        path: 'base/user',
        name: 'UserManage',
        component: () => import('@/views/base/user/index.vue'),
        meta: {
          title: '用户与权限管理',
          group: '用户与权限',
          groupRoute: '/base/user',
          desc: '平台用户建档、角色分配与部门维护。'
        }
      },
      {
        path: 'base/log',
        name: 'OperationLog',
        component: () => import('@/views/base/log/index.vue'),
        meta: {
          title: '操作日志',
          group: '平台管理',
          groupRoute: '/base/user',
          desc: '增删改接口的调用留痕，支持按操作人、模块、状态筛选。'
        }
      },
      {
        path: 'purchase',
        name: 'PurchaseAcceptance',
        component: () => import('@/views/purchase/index.vue'),
        meta: {
          title: '采购验收与入库',
          desc: '该模块由采购管理子系统提供，当前占位。'
        }
      },
      {
        path: 'issue',
        name: 'IssueOutbound',
        component: () => import('@/views/issue/index.vue'),
        meta: {
          title: '领用申请与出库',
          desc: '该模块由领用出库子系统提供，当前占位。'
        }
      },
      {
        path: 'borrow',
        name: 'BorrowDamage',
        component: () => import('@/views/borrow/index.vue'),
        meta: {
          title: '借用归还与报损',
          desc: '该模块由借用报损子系统提供，当前占位。'
        }
      },
      {
        path: 'inventory',
        name: 'InventoryWarning',
        component: () => import('@/views/inventory/index.vue'),
        meta: {
          title: '盘点预警与补货',
          desc: '该模块由盘点预警子系统提供，当前占位。'
        }
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: roleHome
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.afterEach((to) => {
  document.title = to.meta.title
    ? `${to.meta.title} - 学校物资管理系统业务工作台`
    : '学校物资管理系统业务工作台'
})

export default router
