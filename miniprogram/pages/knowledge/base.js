Page({
  data: {
    loading: true,
    selectedCategory: 'ALL',
    searchKeyword: '',
    categories: [
      { id: 'ALL', name: '全部', short: '全' },
      { id: 'SAFETY', name: '安全防护', short: '安' },
      { id: 'NUTRITION', name: '营养膳食', short: '养' },
      { id: 'COGNITIVE', name: '认知照护', short: '认' },
      { id: 'EMOTION', name: '心理关怀', short: '心' },
      { id: 'DAILY', name: '生活照料', short: '护' }
    ],
    articles: [],
    filteredArticles: [],
    featured: null,
    showFeatured: true,
    selectedArticle: null,
    showDetail: false
  },

  onLoad() { this.loadArticles() },

  loadArticles() {
    const articles = this.getArticles().map(item => ({ ...item, categoryName: this.getCategoryName(item.category) }))
    this.setData({ articles, filteredArticles: articles, featured: articles[0], loading: false })
  },

  getArticles() {
    return [
      { id: 1, category: 'SAFETY', title: '养老院老人跌倒预防实用指南', summary: '从环境、穿着、行动辅助和夜间巡视四方面降低跌倒风险。', readTime: '5分钟', level: '重点推荐', tags: ['跌倒预防','环境安全'], content: '一、保持通道整洁\n及时清理地面积水和杂物，卫生间及走廊设置扶手，夜间开启柔和照明。\n\n二、关注老人状态\n起床和改变体位时动作放慢；头晕、乏力或服用镇静药物后，应由护理员陪同活动。\n\n三、正确使用辅助器具\n选择合脚防滑鞋，定期检查轮椅、拐杖和助行器。\n\n四、发生跌倒后\n不要立即搬动老人，先观察意识、疼痛和肢体活动情况，通知照护主管并按流程处理。' },
      { id: 2, category: 'NUTRITION', title: '老年人日常营养与饮水管理', summary: '合理安排蛋白质、蔬果和饮水，识别营养不良的早期信号。', readTime: '6分钟', level: '日常照护', tags: ['营养','饮水'], content: '保证食物多样化，适量增加优质蛋白，结合老人咀嚼和吞咽能力调整软硬度。\n\n每日记录进食量和饮水量。若出现体重持续下降、食欲明显减退、吞咽呛咳或长期腹泻，应及时进行营养评估。\n\n糖尿病、高血压及肾功能异常老人应遵循个体化饮食方案。' },
      { id: 3, category: 'COGNITIVE', title: '失智老人沟通与陪伴技巧', summary: '用简单、尊重且稳定的方式沟通，减少焦虑与行为困扰。', readTime: '5分钟', level: '专项照护', tags: ['失智照护','沟通'], content: '交流时保持视线同高，一次只表达一件事，使用简短清晰的句子并留出反应时间。\n\n不要争辩或反复纠正记忆错误，可先回应情绪，再引导到熟悉、安全的活动。\n\n保持作息、照护人员和生活环境相对稳定，重要物品放在固定位置。' },
      { id: 4, category: 'DAILY', title: '卧床老人皮肤与体位照护', summary: '通过规律翻身、皮肤观察和减压措施预防压疮。', readTime: '7分钟', level: '重点照护', tags: ['皮肤护理','压疮预防'], content: '根据老人风险等级制定翻身计划，翻身时避免拖拉皮肤。每日检查骶尾部、足跟、髋部等受压位置。\n\n保持床单位平整、清洁、干燥，失禁后及时清洁并使用皮肤保护用品。\n\n发现持续不褪色红斑、水疱或破损时，应立即减压并报告照护主管。' },
      { id: 5, category: 'EMOTION', title: '识别老人抑郁与孤独信号', summary: '关注情绪、睡眠、兴趣和社交变化，及时提供心理支持。', readTime: '4分钟', level: '心理关怀', tags: ['情绪','陪伴'], content: '持续情绪低落、回避社交、失眠或嗜睡、食欲变化、反复表达无价值感，可能是需要关注的信号。\n\n主动倾听而非急于说教，鼓励参加适合的活动并保持家属联系。\n\n若老人表达不想活或自伤想法，不得让其独处，应立即启动安全保护流程并寻求专业帮助。' },
      { id: 6, category: 'SAFETY', title: '用药安全：五个必须核对', summary: '核对老人、药物、剂量、时间和给药方式，降低用药差错。', readTime: '4分钟', level: '安全规范', tags: ['用药安全','核对'], content: '给药前必须核对老人身份、药物名称、剂量、给药时间和给药方式。\n\n观察吞咽情况，不得擅自碾碎缓释片或肠溶片。记录实际服药情况和异常反应。\n\n出现皮疹、呼吸困难、意识变化等情况时，应停止给药并立即按应急流程处理。' },
      { id: 7, category: 'DAILY', title: '老人睡眠照护与夜间巡视', summary: '通过规律作息、舒适环境和针对性巡视改善睡眠。', readTime: '3分钟', level: '生活照料', tags: ['睡眠','夜间巡视'], content: '白天安排适量活动并减少长时间午睡，晚间控制浓茶、咖啡和大量饮水。\n\n保持卧室安静、温度适宜，夜间照明以能看清道路且不影响睡眠为宜。\n\n对跌倒高风险、认知障碍或频繁起夜老人增加巡视频率。' },
      { id: 8, category: 'NUTRITION', title: '吞咽困难老人进食照护', summary: '选择合适食物质地和进食姿势，减少呛咳与误吸风险。', readTime: '6分钟', level: '专项照护', tags: ['吞咽','进食安全'], content: '进食前确认老人清醒，取坐位或抬高床头，少量慢喂，每口吞咽完成后再进下一口。\n\n根据评估结果调整食物质地，避免干硬、松散或水和固体混合的食物。\n\n进食中持续咳嗽、声音湿哑、呼吸急促时立即停止，并报告专业人员评估。' }
    ]
  },

  switchCategory(e) { this.setData({ selectedCategory: e.currentTarget.dataset.id }); this.applyFilter() },
  onSearchInput(e) { this.setData({ searchKeyword: e.detail.value }); this.applyFilter() },
  clearSearch() { this.setData({ searchKeyword: '' }); this.applyFilter() },
  applyFilter() {
    const category = this.data.selectedCategory
    const keyword = this.data.searchKeyword.trim().toLowerCase()
    const filtered = this.data.articles.filter(item => {
      const categoryMatched = category === 'ALL' || item.category === category
      const keywordMatched = !keyword || `${item.title}${item.summary}${item.tags.join('')}`.toLowerCase().includes(keyword)
      return categoryMatched && keywordMatched
    })
    this.setData({
      filteredArticles: filtered,
      showFeatured: category === 'ALL' && !keyword
    })
  },
  getCategoryName(id) { const item = this.data.categories.find(category => category.id === id); return item ? item.name : '养老照护' },
  viewArticle(e) { const article = this.data.articles.find(item => item.id === Number(e.currentTarget.dataset.id)); if (article) this.setData({ selectedArticle: article, showDetail: true }) },
  viewFeatured() { if (this.data.featured) this.setData({ selectedArticle: this.data.featured, showDetail: true }) },
  closeDetail() { this.setData({ showDetail: false, selectedArticle: null }) },
  stopPropagation() {},
  onPullDownRefresh() { this.loadArticles(); wx.stopPullDownRefresh() }
})
