const app = getApp()

Page({
  data: {
    messageList: [],
    unreadCount: 0,
    loading: false
  },

  onLoad() {
    this.loadMessages()
  },

  onShow() {
    this.loadMessages()
  },

  // 加载消息列表
  loadMessages() {
    this.setData({ loading: true })
    
    app.request('/alert/list', 'GET').then(res => {
      const messages = res || []
      const unreadCount = messages.filter(m => m.isRead === 0).length
      
      this.setData({ 
        messageList: messages,
        unreadCount
      })
    }).catch(err => {
      console.error('加载消息失败:', err)
      wx.showToast({ title: '加载失败', icon: 'none' })
    }).finally(() => {
      this.setData({ loading: false })
    })
  },

  // 查看消息详情
  viewMessage(e) {
    const { id, residentId } = e.currentTarget.dataset
    
    // 标记为已读
    this.markAsRead(id)
    
    // 跳转到老人详情或评估页面
    if (residentId) {
      wx.navigateTo({
        url: `/pages/resident/detail/detail?id=${residentId}`
      })
    }
  },

  // 标记为已读
  markAsRead(messageId) {
    app.request('/alert/read', 'POST', { id: messageId }).then(() => {
      // 更新本地数据
      const messageList = this.data.messageList.map(msg => {
        if (msg.id === messageId) {
          return { ...msg, isRead: 1 }
        }
        return msg
      })
      
      const unreadCount = messageList.filter(m => m.isRead === 0).length
      
      this.setData({ 
        messageList,
        unreadCount
      })
    }).catch(err => {
      console.error('标记已读失败:', err)
    })
  },

  // 全部标记为已读
  markAllAsRead() {
    wx.showModal({
      title: '确认操作',
      content: '确认将所有消息标记为已读吗？',
      success: (res) => {
        if (res.confirm) {
          app.request('/alert/readAll', 'POST').then(() => {
            const messageList = this.data.messageList.map(msg => ({
              ...msg,
              isRead: 1
            }))
            
            this.setData({ 
              messageList,
              unreadCount: 0
            })
            
            wx.showToast({ title: '已全部标记为已读', icon: 'success' })
          }).catch(err => {
            wx.showToast({ title: '操作失败', icon: 'none' })
          })
        }
      }
    })
  },

  // 删除消息
  deleteMessage(e) {
    const id = e.currentTarget.dataset.id
    
    wx.showModal({
      title: '确认删除',
      content: '确认删除该消息吗？',
      success: (res) => {
        if (res.confirm) {
          app.request('/alert/delete', 'POST', { id }).then(() => {
            const messageList = this.data.messageList.filter(msg => msg.id !== id)
            const unreadCount = messageList.filter(m => m.isRead === 0).length
            
            this.setData({ 
              messageList,
              unreadCount
            })
            
            wx.showToast({ title: '已删除', icon: 'success' })
          }).catch(err => {
            wx.showToast({ title: '删除失败', icon: 'none' })
          })
        }
      }
    })
  },

  // 下拉刷新
  onPullDownRefresh() {
    this.loadMessages()
    setTimeout(() => {
      wx.stopPullDownRefresh()
    }, 1000)
  }
})
