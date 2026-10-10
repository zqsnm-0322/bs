const app = getApp()

Page({
  data: {
    userInfo: {},
    conversations: [
      {
        id: 1,
        staffName: '张主管',
        staffTitle: '照护主管',
        lastMessage: '您的血压控制得不错，继续按时服药',
        lastTime: '2026-02-02 15:30',
        unreadCount: 0,
        status: 'active',
        avatar: '👨‍💼'
      },
      {
        id: 2,
        staffName: '李护理员',
        staffTitle: '责任护理员',
        lastMessage: '今天记得按时测量血压哦',
        lastTime: '2026-02-02 09:15',
        unreadCount: 2,
        status: 'active',
        avatar: '👩‍💼'
      }
    ],
    messages: [
      {
        id: 1,
        conversationId: 1,
        senderId: 'care_1',
        senderName: '张主管',
        senderType: 'caregiver',
        content: '您好，我是张主管。生活照护方面的问题可以随时联系我。',
        timestamp: '2026-02-01 14:00',
        type: 'text'
      },
      {
        id: 2,
        conversationId: 1,
        senderId: 'resident_1',
        senderName: '老人',
        senderType: 'resident',
        content: '您好，我今天有些头晕，生活照护上需要注意什么？',
        timestamp: '2026-02-01 14:05',
        type: 'text'
      },
      {
        id: 3,
        conversationId: 1,
        senderId: 'care_1',
        senderName: '张主管',
        senderType: 'caregiver',
        content: '请先坐下休息，避免独自行走。我会安排护理员陪同测量生命体征，如持续不适会及时联系家属并协助就医。',
        timestamp: '2026-02-01 14:10',
        type: 'text'
      },
      {
        id: 4,
        conversationId: 1,
        senderId: 'resident_1',
        senderName: '老人',
        senderType: 'resident',
        content: '谢谢您的提醒，我会注意的',
        timestamp: '2026-02-01 14:15',
        type: 'text'
      },
      {
        id: 5,
        conversationId: 1,
        senderId: 'care_1',
        senderName: '张主管',
        senderType: 'caregiver',
        content: '今天状态平稳，请继续按照照护计划作息和用药',
        timestamp: '2026-02-02 15:30',
        type: 'text'
      }
    ],
    quickReplies: [
      '谢谢您的帮助',
      '我知道了',
      '还有其他问题',
      '需要复查吗？',
      '用药有副作用',
      '症状有改善'
    ],
    currentConversation: null,
    inputMessage: '',
    activeTab: 'conversations',
    sending: false
  },

  onLoad(options) {
    this.loadUserInfo()
    
    // 如果有传入对话ID，直接打开对话
    if (options.conversationId) {
      this.openConversation({ currentTarget: { dataset: { id: options.conversationId } } })
    }
  },

  loadUserInfo() {
    const userInfo = app.globalData.userInfo || {}
    this.setData({ userInfo })
  },

  switchTab(e) {
    const tab = e.currentTarget.dataset.tab
    this.setData({ activeTab: tab })
  },

  openConversation(e) {
    const conversationId = parseInt(e.currentTarget.dataset.id)
    const conversation = this.data.conversations.find(c => c.id === conversationId)
    
    if (conversation) {
      // 标记为已读
      const updatedConversations = this.data.conversations.map(c => {
        if (c.id === conversationId) {
          return { ...c, unreadCount: 0 }
        }
        return c
      })
      
      // 获取该对话的消息
      const conversationMessages = this.data.messages.filter(m => m.conversationId === conversationId)
      
      this.setData({
        currentConversation: conversation,
        conversations: updatedConversations,
        messages: conversationMessages,
        activeTab: 'chat'
      })
    }
  },

  backToConversations() {
    this.setData({
      currentConversation: null,
      activeTab: 'conversations'
    })
  },

  onInputMessage(e) {
    this.setData({ inputMessage: e.detail.value })
  },

  sendMessage() {
    const message = this.data.inputMessage.trim()
    if (!message || this.data.sending) {
      return
    }
    
    this.setData({ sending: true })
    
    const newMessage = {
      id: Date.now(),
      conversationId: this.data.currentConversation.id,
      senderId: this.data.userInfo.id || 'resident_1',
      senderName: this.data.userInfo.name || '老人',
      senderType: 'resident',
      content: message,
      timestamp: new Date().toLocaleString(),
      type: 'text'
    }
    
    // 添加消息到列表
    const updatedMessages = [...this.data.messages, newMessage]
    
    // 更新对话列表中的最后消息
    const updatedConversations = this.data.conversations.map(c => {
      if (c.id === this.data.currentConversation.id) {
        return {
          ...c,
          lastMessage: message,
          lastTime: newMessage.timestamp
        }
      }
      return c
    })
    
    this.setData({
      messages: updatedMessages,
      conversations: updatedConversations,
      inputMessage: '',
      sending: false
    })
    
    // 模拟照护人员回复
    this.simulateCareReply()
    
    // 滚动到底部
    setTimeout(() => {
      this.scrollToBottom()
    }, 100)
  },

  simulateCareReply() {
    const replies = [
      '我已经收到您的消息，稍后会详细回复您',
      '根据您的描述，建议您注意休息',
      '这种情况比较常见，不用太担心',
      '请按照用药提醒执行，有异常及时联系护理员',
      '今天的状态不错，请继续保持规律作息'
    ]
    
    setTimeout(() => {
      const careReply = {
        id: Date.now(),
        conversationId: this.data.currentConversation.id,
        senderId: 'care_1',
        senderName: this.data.currentConversation.staffName,
        senderType: 'caregiver',
        content: replies[Math.floor(Math.random() * replies.length)],
        timestamp: new Date().toLocaleString(),
        type: 'text'
      }
      
      const updatedMessages = [...this.data.messages, careReply]
      const updatedConversations = this.data.conversations.map(c => {
        if (c.id === this.data.currentConversation.id) {
          return {
            ...c,
            lastMessage: careReply.content,
            lastTime: careReply.timestamp,
            unreadCount: c.unreadCount + 1
          }
        }
        return c
      })
      
      this.setData({
        messages: updatedMessages,
        conversations: updatedConversations
      })
      
      this.scrollToBottom()
    }, 2000 + Math.random() * 3000) // 2-5秒后回复
  },

  sendQuickReply(e) {
    const reply = e.currentTarget.dataset.reply
    this.setData({ inputMessage: reply })
    this.sendMessage()
  },

  scrollToBottom() {
    wx.createSelectorQuery().select('.messages-list').boundingClientRect((rect) => {
      if (rect) {
        wx.pageScrollTo({
          scrollTop: rect.bottom,
          duration: 300
        })
      }
    }).exec()
  },

  startNewConsultation() {
    wx.showModal({
      title: '发起咨询',
      content: '是否要发起新的在线咨询？',
      success: (res) => {
        if (res.confirm) {
          wx.showToast({
            title: '功能开发中',
            icon: 'none'
          })
        }
      }
    })
  },

  makePhoneCall(e) {
    const phone = e.currentTarget.dataset.phone || '400-123-4567'
    wx.makePhoneCall({
      phoneNumber: phone
    })
  },

  getMessageTime(timestamp) {
    const now = new Date()
    const msgTime = new Date(timestamp)
    const diff = now - msgTime
    
    if (diff < 60000) { // 1分钟内
      return '刚刚'
    } else if (diff < 3600000) { // 1小时内
      return Math.floor(diff / 60000) + '分钟前'
    } else if (diff < 86400000) { // 24小时内
      return Math.floor(diff / 3600000) + '小时前'
    } else {
      return msgTime.toLocaleDateString()
    }
  },

  onPullDownRefresh() {
    this.loadUserInfo()
    setTimeout(() => {
      wx.stopPullDownRefresh()
    }, 1000)
  }
})
