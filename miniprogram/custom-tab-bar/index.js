Component({
  data: {
    selected: 0,
    color: "#999999",
    selectedColor: "#409EFF",
    list: [
      {
        pagePath: "/pages/index/index",
        text: "首页",
        iconPath: "/images/icon-home.png",
        selectedIconPath: "/images/icon-home-active.png",
        icon: "🏠"
      },
      {
        pagePath: "/pages/tools/index",
        text: "工具",
        iconPath: "/images/icon-tools.png",
        selectedIconPath: "/images/icon-tools-active.png",
        icon: "🔧"
      },
      {
        pagePath: "/pages/mine/mine",
        text: "我的",
        iconPath: "/images/icon-mine.png",
        selectedIconPath: "/images/icon-mine-active.png",
        icon: "👤"
      }
    ]
  },
  
  attached() {
  },
  
  methods: {
    switchTab(e) {
      const data = e.currentTarget.dataset
      const url = data.path
      wx.switchTab({url})
      this.setData({
        selected: data.index
      })
    }
  }
})
