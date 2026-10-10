const app = getApp()

Page({
  data: {
    categories: [
      { id: 'fall', name: '跌倒预防', icon: '🚶', color: '#667eea' },
      { id: 'pressure', name: '压疮预防', icon: '🛏️', color: '#f093fb' },
      { id: 'cognitive', name: '认知训练', icon: '🧩', color: '#4facfe' },
      { id: 'medication', name: '用药指导', icon: '💊', color: '#43e97b' },
      { id: 'nutrition', name: '营养指导', icon: '🥗', color: '#ffa726' },
      { id: 'exercise', name: '康复锻炼', icon: '🏃', color: '#ab47bc' }
    ],
    educationList: [
      {
        id: 1,
        category: 'fall',
        categoryIcon: '🚶',
        title: '如何预防跌倒',
        summary: '了解跌倒的风险因素和预防措施',
        content: '跌倒是入住老人常见的安全问题。预防措施包括：\n\n1. 保持环境整洁\n- 及时清理地面积水\n- 移除障碍物\n- 保持过道畅通\n\n2. 正确使用辅助设备\n- 穿防滑鞋\n- 使用扶手\n- 必要时使用助行器\n\n3. 注意活动安全\n- 起床时缓慢坐起\n- 避免突然改变体位\n- 夜间如厕开灯\n\n4. 及时求助\n- 感到头晕时立即坐下\n- 需要帮助时按呼叫铃\n- 不要勉强独自行走',
        readTime: '3分钟',
        publishDate: '2026-01-15',
        views: 156,
        isRead: false
      },
      {
        id: 2,
        category: 'pressure',
        categoryIcon: '🛏️',
        title: '压疮的预防与护理',
        summary: '学习压疮形成原因及预防方法',
        content: '压疮是由于局部组织长期受压导致的皮肤损伤。预防要点：\n\n1. 定时翻身\n- 每2小时翻身一次\n- 避免拖拽皮肤\n- 使用翻身枕辅助\n\n2. 保持皮肤清洁干燥\n- 及时清洁污染的皮肤\n- 使用温和的清洁用品\n- 涂抹润肤霜保护皮肤\n\n3. 减轻压力\n- 使用气垫床\n- 避免长时间保持同一姿势\n- 穿宽松舒适的衣物\n\n4. 营养支持\n- 保证充足的蛋白质摄入\n- 多吃富含维生素的食物\n- 保持充足的水分摄入',
        readTime: '4分钟',
        publishDate: '2026-01-20',
        views: 89,
        isRead: true
      },
      {
        id: 3,
        category: 'medication',
        categoryIcon: '💊',
        title: '安全用药指导',
        summary: '掌握正确的用药方法和注意事项',
        content: '安全用药是治疗成功的关键。用药要点：\n\n1. 遵医嘱用药\n- 按时按量服药\n- 不可随意增减剂量\n- 不可擅自停药\n\n2. 了解药物信息\n- 记住药物名称和作用\n- 了解常见副作用\n- 知晓用药禁忌\n\n3. 正确服药方法\n- 用温开水送服\n- 注意服药时间\n- 特殊药物按要求服用\n\n4. 观察药物反应\n- 注意身体变化\n- 及时报告不良反应\n- 定期复查相关指标',
        readTime: '5分钟',
        publishDate: '2026-01-25',
        views: 234,
        isRead: false
      }
    ],
    selectedCategory: 'all',
    searchKeyword: ''
  },

  onLoad() {
    this.loadEducationData()
  },

  loadEducationData() {
    // 这里可以调用后端API获取宣教内容
    // 目前使用模拟数据
  },

  selectCategory(e) {
    const category = e.currentTarget.dataset.category
    this.setData({ selectedCategory: category })
    this.filterEducationList()
  },

  onSearchInput(e) {
    this.setData({ searchKeyword: e.detail.value })
    this.filterEducationList()
  },

  filterEducationList() {
    // 这里可以实现筛选逻辑
    // 目前显示所有数据
  },

  viewEducation(e) {
    const id = e.currentTarget.dataset.id
    const education = this.data.educationList.find(item => item.id === id)
    
    if (education) {
      // 标记为已读
      const updatedList = this.data.educationList.map(item => {
        if (item.id === id) {
          return { ...item, isRead: true, views: item.views + 1 }
        }
        return item
      })
      
      this.setData({ educationList: updatedList })
      
      // 跳转到详情页面
      wx.navigateTo({
        url: `/pages/health/education-detail?id=${id}&title=${education.title}&content=${encodeURIComponent(education.content)}`
      })
    }
  },

  getCategoryColor(category) {
    const categoryMap = {
      'fall': '#667eea',
      'pressure': '#f093fb',
      'cognitive': '#4facfe',
      'medication': '#43e97b',
      'nutrition': '#ffa726',
      'exercise': '#ab47bc'
    }
    return categoryMap[category] || '#667eea'
  },

  onPullDownRefresh() {
    this.loadEducationData()
    setTimeout(() => {
      wx.stopPullDownRefresh()
    }, 1000)
  }
})
