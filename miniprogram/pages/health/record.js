const app = getApp()

Page({
  data: {
    userInfo: {},
    residentInfo: {
      name: '',
      residentNo: '',
      bedNo: '',
      age: 0,
      gender: '',
      admitDate: '',
      healthLevel: '',
      careZone: ''
    },
    riskRecords: [],
    vitalSigns: [],
    medications: [],
    healthPredictions: [],
    assessmentTime: '',
    activeTab: 'basic',
    shareImagePath: '', // 用于分享的图片路径
    predictionsLoading: false
  },

  onLoad() {
    // 获取用户信息
    const userInfo = app.globalData.userInfo || wx.getStorageSync('userInfo') || {}
    this.setData({ userInfo })
    
    this.loadResidentData()
    this.loadRiskRecords()
    this.loadVitalSigns()
    this.loadMedications()
    this.loadHealthPredictions()
  },

  loadResidentData() {
    const userInfo = app.globalData.userInfo || {}
    
    // 如果有老人ID，从后端获取完整信息
    if (userInfo.residentId || userInfo.id) {
      const residentId = userInfo.residentId || userInfo.id
      
      wx.showLoading({ title: '加载中...' })
      
      app.request(`/resident/${residentId}`, 'GET').then(res => {
        const resident = app.normalizeUser(res)
        
        const residentInfo = {
          name: resident.name || userInfo.name || '',
          residentNo: resident.residentNo || resident.residentId || userInfo.residentNo || userInfo.residentId || '',
          bedNo: resident.roomBedNo || userInfo.roomBedNo || '未分配',
          age: resident.age || userInfo.age || 0,
          gender: resident.gender || userInfo.gender || '',
          admitDate: resident.admitTime ? this.formatDate(resident.admitTime) : '未知',
          healthLevel: resident.healthLevel || userInfo.healthLevel || '待评估',
          careZone: resident.careZone || userInfo.careZone || '待分配'
        }
        
        this.setData({
          userInfo,
          residentInfo
        })
        
        wx.hideLoading()
      }).catch(err => {
        console.error('获取老人信息失败:', err)
        wx.hideLoading()
        
        // 使用本地缓存的信息
        this.setLocalResidentData(userInfo)
      })
    } else {
      // 没有老人ID，使用本地信息
      this.setLocalResidentData(userInfo)
    }
  },
  
  setLocalResidentData(userInfo) {
    const residentInfo = {
      name: userInfo.name || '',
      residentNo: userInfo.residentNo || userInfo.residentId || '',
      bedNo: userInfo.roomBedNo || userInfo.bedNo || '未分配',
      age: userInfo.age || 0,
      gender: userInfo.gender || '',
      admitDate: '未知',
      healthLevel: userInfo.healthLevel || '待评估',
      careZone: userInfo.careZone || '待分配'
    }

    this.setData({
      userInfo,
      residentInfo
    })
  },
  
  // 格式化日期
  formatDate(dateStr) {
    if (!dateStr) return '未知'
    
    try {
      const date = new Date(dateStr)
      const year = date.getFullYear()
      const month = String(date.getMonth() + 1).padStart(2, '0')
      const day = String(date.getDate()).padStart(2, '0')
      return `${year}-${month}-${day}`
    } catch (e) {
      return dateStr
    }
  },

  // 老人可以查看自己的风险评估报告
  loadRiskRecords() {
    const userInfo = app.globalData.userInfo || {}
    const residentId = userInfo.residentId || userInfo.id
    
    if (!residentId) {
      console.log('没有老人ID，无法加载风险评估记录')
      return
    }

    wx.showLoading({ title: '加载评估记录...' })
    
    // 从后端获取风险评估记录
    app.request(`/enhanced-risk/history/${residentId}`, 'GET').then(res => {
      wx.hideLoading()
      
      if (res && Array.isArray(res)) {
        // 处理数据格式
        const riskRecords = res.map(record => ({
          id: record.id,
          date: this.formatDate(record.assessmentTime),
          scaleType: record.assessmentType || 'COMPREHENSIVE',
          scaleTypeName: record.assessmentType || '综合风险评估',
          riskLevel: record.riskLevel,
          riskLevelText: record.riskLevelText || this.getRiskLevelText(record.riskLevel),
          totalScore: record.totalScore,
          evaluatorName: record.evaluatorName || '系统评估',
          interventions: [] // 后端暂时没有返回干预措施
        }))
        
        this.setData({ riskRecords })
      } else if (res && res.records) {
        // 处理分页数据格式
        const riskRecords = res.records.map(record => ({
          id: record.id,
          date: this.formatDate(record.assessmentTime),
          scaleType: record.assessmentType || 'COMPREHENSIVE',
          scaleTypeName: record.assessmentType || '综合风险评估',
          riskLevel: record.riskLevel,
          riskLevelText: record.riskLevelText || this.getRiskLevelText(record.riskLevel),
          totalScore: record.totalScore,
          evaluatorName: record.evaluatorName || '系统评估',
          interventions: []
        }))
        
        this.setData({ riskRecords })
      } else {
        this.setData({ riskRecords: [] })
      }
    }).catch(err => {
      wx.hideLoading()
      console.error('加载风险评估记录失败:', err)
      this.setData({ riskRecords: [] })
    })
  },

  // 获取量表类型名称
  getScaleTypeName(scaleType) {
    const typeMap = {
      'FALL': '跌倒风险评估',
      'PRESSURE': '压疮风险评估',
      'NUTRITION': '营养风险评估',
      'COGNITIVE': '认知能力评估',
      'WANDERING': '走失风险评估'
    }
    return typeMap[scaleType] || scaleType
  },

  // 获取风险等级文本
  getRiskLevelText(riskLevel) {
    const levelMap = {
      'LOW': '低危',
      'MEDIUM': '中危',
      'HIGH': '高危'
    }
    return levelMap[riskLevel] || riskLevel
  },

  switchTab(e) {
    const tab = e.currentTarget.dataset.tab
    this.setData({ activeTab: tab })
  },

  getRiskColor(level) {
    const colorMap = {
      'LOW': '#52c41a',
      'MEDIUM': '#faad14',
      'HIGH': '#f5222d'
    }
    return colorMap[level] || '#52c41a'
  },

  viewRiskDetail(e) {
    const id = e.currentTarget.dataset.id
    wx.showModal({
      title: '评估详情',
      content: '查看详细的风险评估报告和护理建议',
      showCancel: false
    })
  },

  exportRecord() {
    wx.showLoading({
      title: '正在生成...',
      mask: true
    })

    // 使用Canvas生成健康档案图片
    this.generateHealthRecordImage()
  },

  generateHealthRecordImage() {
    const query = wx.createSelectorQuery()
    query.select('.record-page').boundingClientRect()
    query.exec((res) => {
      if (!res[0]) {
        wx.hideLoading()
        wx.showToast({ title: '生成失败', icon: 'none' })
        return
      }

      // 创建Canvas上下文
      const ctx = wx.createCanvasContext('healthRecordCanvas')
      const canvasWidth = 750
      const canvasHeight = 1200
      
      // 设置背景
      ctx.setFillStyle('#f5f5f5')
      ctx.fillRect(0, 0, canvasWidth, canvasHeight)
      
      // 绘制标题
      ctx.setFillStyle('#ffffff')
      ctx.fillRect(0, 0, canvasWidth, 200)
      ctx.setFillStyle('#667eea')
      ctx.setFontSize(40)
      ctx.setTextAlign('center')
      ctx.fillText('健康档案', canvasWidth / 2, 80)
      
      // 绘制老人信息
      ctx.setFillStyle('#333333')
      ctx.setFontSize(32)
      ctx.setTextAlign('left')
      ctx.fillText(`姓名: ${this.data.residentInfo.name}`, 50, 150)
      ctx.fillText(`入住编号: ${this.data.residentInfo.residentNo}`, 400, 150)
      
      // 绘制基本信息卡片
      let yPos = 240
      ctx.setFillStyle('#ffffff')
      ctx.fillRect(30, yPos, canvasWidth - 60, 400)
      
      ctx.setFillStyle('#333333')
      ctx.setFontSize(28)
      ctx.fillText('基本信息', 50, yPos + 40)
      
      yPos += 80
      const infoItems = [
        { label: '床号', value: this.data.residentInfo.bedNo },
        { label: '年龄', value: `${this.data.residentInfo.age}岁` },
        { label: '性别', value: this.data.residentInfo.gender },
        { label: '护理区', value: this.data.residentInfo.careZone },
        { label: '健康等级', value: this.data.residentInfo.healthLevel || '待评估' },
        { label: '入住日期', value: this.data.residentInfo.admitDate }
      ]
      
      ctx.setFontSize(24)
      infoItems.forEach((item, index) => {
        const row = Math.floor(index / 2)
        const col = index % 2
        const x = 50 + col * 350
        const y = yPos + row * 50
        
        ctx.setFillStyle('#999999')
        ctx.fillText(item.label + ':', x, y)
        ctx.setFillStyle('#333333')
        ctx.fillText(item.value, x + 100, y)
      })
      
      // 绘制风险评估
      yPos += 200
      ctx.setFillStyle('#ffffff')
      ctx.fillRect(30, yPos, canvasWidth - 60, 300)
      
      ctx.setFillStyle('#333333')
      ctx.setFontSize(28)
      ctx.fillText('最近风险评估', 50, yPos + 40)
      
      yPos += 80
      if (this.data.riskRecords.length > 0) {
        const latestRisk = this.data.riskRecords[0]
        ctx.setFontSize(24)
        ctx.setFillStyle('#666666')
        ctx.fillText(`${latestRisk.scaleTypeName}`, 50, yPos)
        ctx.fillText(`评分: ${latestRisk.totalScore}分`, 50, yPos + 40)
        
        // 风险等级
        const riskColor = this.getRiskColor(latestRisk.riskLevel)
        ctx.setFillStyle(riskColor)
        ctx.fillText(`风险等级: ${latestRisk.riskLevelText}`, 50, yPos + 80)
        
        ctx.setFillStyle('#666666')
        ctx.fillText(`评估日期: ${latestRisk.date}`, 50, yPos + 120)
      }
      
      // 绘制二维码(可选)
      ctx.setFillStyle('#999999')
      ctx.setFontSize(20)
      ctx.fillText('扫码查看完整档案', 50, canvasHeight - 50)
      
      // 绘制完成,生成图片
      ctx.draw(false, () => {
        setTimeout(() => {
          wx.canvasToTempFilePath({
            canvasId: 'healthRecordCanvas',
            success: (res) => {
              wx.hideLoading()
              this.saveOrShareImage(res.tempFilePath)
            },
            fail: (err) => {
              wx.hideLoading()
              console.error('生成图片失败:', err)
              wx.showToast({ title: '生成失败', icon: 'none' })
            }
          })
        }, 500)
      })
    })
  },

  saveOrShareImage(imagePath) {
    wx.showActionSheet({
      itemList: ['保存到相册', '分享给好友'],
      success: (res) => {
        if (res.tapIndex === 0) {
          // 保存到相册
          wx.saveImageToPhotosAlbum({
            filePath: imagePath,
            success: () => {
              wx.showToast({ title: '已保存到相册', icon: 'success' })
            },
            fail: (err) => {
              if (err.errMsg.includes('auth deny')) {
                wx.showModal({
                  title: '提示',
                  content: '需要授权保存到相册',
                  success: (modalRes) => {
                    if (modalRes.confirm) {
                      wx.openSetting()
                    }
                  }
                })
              } else {
                wx.showToast({ title: '保存失败', icon: 'none' })
              }
            }
          })
        } else if (res.tapIndex === 1) {
          // 分享给好友
          this.setData({ shareImagePath: imagePath })
          wx.showToast({ title: '请点击右上角分享', icon: 'none' })
        }
      }
    })
  },

  // 分享功能
  onShareAppMessage() {
    if (this.data.shareImagePath) {
      return {
        title: `${this.data.residentInfo.name}的健康档案`,
        path: '/pages/health/record',
        imageUrl: this.data.shareImagePath
      }
    }
    return {
      title: '健康档案',
      path: '/pages/health/record'
    }
  },

  loadVitalSigns() {
    const userInfo = app.globalData.userInfo || {}
    const residentId = userInfo.residentId || userInfo.id
    
    if (!residentId) {
      console.log('没有老人ID，无法加载生命体征')
      return
    }

    app.request(`/vital-signs/resident/${residentId}?limit=10`, 'GET').then(res => {
      if (res && Array.isArray(res)) {
        this.setData({ vitalSigns: res })
      } else {
        this.setData({ vitalSigns: [] })
      }
    }).catch(err => {
      console.error('加载生命体征失败:', err)
      this.setData({ vitalSigns: [] })
    })
  },

  // 需求8: 用药记录添加结束日期
  loadMedications() {
    const userInfo = app.globalData.userInfo || {}
    const residentId = userInfo.residentId || userInfo.id
    
    if (!residentId) {
      console.log('没有老人ID，无法加载用药记录')
      return
    }

    app.request(`/medication/resident/${residentId}`, 'GET').then(res => {
      if (res && Array.isArray(res)) {
        const medications = res.map(med => ({
          id: med.id,
          name: med.medicationName || med.name,
          dosage: med.dosage,
          frequency: med.frequency,
          startDate: med.startDate,
          endDate: med.endDate || null, // 需求8: 添加结束日期
          status: this.getMedicationStatusText(med.status),
          statusColor: this.getMedicationStatusColor(med.status)
        }))
        this.setData({ medications })
      } else {
        this.setData({ medications: [] })
      }
    }).catch(err => {
      console.error('加载用药记录失败:', err)
      this.setData({ medications: [] })
    })
  },

  getMedicationStatusText(status) {
    const statusMap = {
      'ACTIVE': '进行中',
      'PAUSED': '已暂停',
      'COMPLETED': '已完成',
      'STOPPED': '已停用'
    }
    return statusMap[status] || status
  },

  getMedicationStatusColor(status) {
    const colorMap = {
      'ACTIVE': '#52c41a',
      'PAUSED': '#faad14',
      'COMPLETED': '#999999',
      'STOPPED': '#f5222d'
    }
    return colorMap[status] || '#52c41a'
  },

  // 添加用药
  addMedication() {
    const userInfo = app.globalData.userInfo || {}
    const residentId = userInfo.residentId || userInfo.id
    
    if (!residentId) {
      wx.showToast({
        title: '老人信息缺失',
        icon: 'none'
      })
      return
    }

    wx.navigateTo({
      url: `/pages/health/add-medication?residentId=${residentId}`
    })
  },

  // 编辑用药
  editMedication(e) {
    const { id } = e.currentTarget.dataset
    const userInfo = app.globalData.userInfo || {}
    const residentId = userInfo.residentId || userInfo.id
    
    wx.navigateTo({
      url: `/pages/health/add-medication?id=${id}&residentId=${residentId}`
    })
  },

  // 停用药品
  stopMedication(e) {
    const { id, name } = e.currentTarget.dataset
    
    wx.showModal({
      title: '确认停用',
      content: `确定要停用药品"${name}"吗？停用后将无法恢复。`,
      editable: true,
      placeholderText: '请输入停用原因(可选)',
      success: (res) => {
        if (res.confirm) {
          const stopReason = res.content || ''
          this.performStopMedication(id, stopReason)
        }
      }
    })
  },

  // 执行停用操作
  performStopMedication(medicationId, stopReason) {
    wx.showLoading({ title: '停用中...', mask: true })

    const requestData = stopReason ? { stopReason } : {}

    app.request(`/medication/${medicationId}/stop`, 'PUT', requestData).then(() => {
      wx.hideLoading()
      wx.showToast({
        title: '停用成功',
        icon: 'success'
      })
      
      // 重新加载用药信息
      this.loadMedications()
    }).catch(err => {
      wx.hideLoading()
      console.error('停用药品失败:', err)
      wx.showToast({
        title: '停用失败',
        icon: 'none'
      })
    })
  },

  // 恢复药品
  resumeMedication(e) {
    const { id, name } = e.currentTarget.dataset
    
    wx.showModal({
      title: '确认恢复',
      content: `确定要恢复药品"${name}"的用药吗？`,
      success: (res) => {
        if (res.confirm) {
          this.performResumeMedication(id)
        }
      }
    })
  },

  // 执行恢复操作
  performResumeMedication(medicationId) {
    wx.showLoading({ title: '恢复中...', mask: true })

    app.request(`/medication/${medicationId}/resume`, 'PUT').then(() => {
      wx.hideLoading()
      wx.showToast({
        title: '恢复成功',
        icon: 'success'
      })
      
      // 重新加载用药信息
      this.loadMedications()
    }).catch(err => {
      wx.hideLoading()
      console.error('恢复药品失败:', err)
      wx.showToast({
        title: '恢复失败',
        icon: 'none'
      })
    })
  },

  onPullDownRefresh() {
    this.loadResidentData()
    this.loadRiskRecords()
    this.loadVitalSigns()
    this.loadMedications()
    this.loadHealthPredictions()
    setTimeout(() => {
      wx.stopPullDownRefresh()
    }, 1000)
  },

  // 加载健康预测
  loadHealthPredictions() {
    const userInfo = app.globalData.userInfo || {}
    const residentId = userInfo.residentId || userInfo.id
    
    if (!residentId) {
      console.log('没有老人ID，无法加载健康预测')
      return
    }

    this.setData({ predictionsLoading: true })
    
    app.request(`/enhanced-risk/predictions/${residentId}`).then(res => {
      if (res && res.predictions) {
        this.setData({
          healthPredictions: res.predictions || [],
          assessmentTime: this.formatDateTime(res.assessmentTime),
          predictionsLoading: false
        })
      } else {
        this.setData({
          healthPredictions: [],
          assessmentTime: '',
          predictionsLoading: false
        })
      }
    }).catch(err => {
      console.error('加载健康预测失败:', err)
      this.setData({
        healthPredictions: [],
        assessmentTime: '',
        predictionsLoading: false
      })
    })
  },

  // 格式化日期时间
  formatDateTime(dateTime) {
    if (!dateTime) return '-'
    try {
      const date = new Date(dateTime)
      const year = date.getFullYear()
      const month = String(date.getMonth() + 1).padStart(2, '0')
      const day = String(date.getDate()).padStart(2, '0')
      const hours = String(date.getHours()).padStart(2, '0')
      const minutes = String(date.getMinutes()).padStart(2, '0')
      return `${year}-${month}-${day} ${hours}:${minutes}`
    } catch (e) {
      return dateTime
    }
  },

  // 查看预测详情
  viewPredictionDetail(e) {
    const prediction = e.currentTarget.dataset.prediction
    if (prediction) {
      let content = `发生概率：${prediction.probability}\n`
      content += `时间窗口：${prediction.timeframe}\n\n`
      
      if (prediction.symptoms && prediction.symptoms.length > 0) {
        content += `可能症状：\n${prediction.symptoms.map(s => `• ${s}`).join('\n')}\n\n`
      }
      
      if (prediction.interventions && prediction.interventions.length > 0) {
        content += `建议措施：\n${prediction.interventions.map(i => `• ${i}`).join('\n')}`
      }
      
      wx.showModal({
        title: prediction.condition,
        content: content,
        showCancel: false,
        confirmText: '知道了'
      })
    }
  },

  // 查看所有预测
  viewAllPredictions() {
    const userInfo = app.globalData.userInfo || {}
    const residentId = userInfo.residentId || userInfo.id
    
    wx.navigateTo({
      url: `/pages/health/predictions?residentId=${residentId}`
    })
  }
})
