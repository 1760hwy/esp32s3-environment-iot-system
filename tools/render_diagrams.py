from pathlib import Path
import shutil
from PIL import Image, ImageDraw, ImageFont

ROOT=Path(__file__).resolve().parents[1]

def put(path,text):
    p=ROOT/path;p.parent.mkdir(exist_ok=True,parents=True);p.write_text(text,encoding='utf-8',newline='\n')

assets=ROOT/'docs/assets';assets.mkdir(parents=True,exist_ok=True)
import argparse
parser=argparse.ArgumentParser(description='Regenerate the source-derived Chinese diagrams; requires Pillow.')
parser.add_argument('--font', default=r'C:\Windows\Fonts\msyh.ttc', help='Path to a font with Chinese glyphs')
font=Path(parser.parse_args().font)
if not font.exists():
    raise SystemExit('Provide an installed Chinese font using --font')
def figure(name,title,boxes,arrows,footer,size=(1600,850)):
    im=Image.new('RGB',size,'#f5f9fa');d=ImageDraw.Draw(im)
    f=lambda n:ImageFont.truetype(str(font),n)
    d.text((55,32),title,fill='#183d38',font=f(38))
    for x1,y1,x2,y2,label,body in boxes:
        d.rounded_rectangle((x1,y1,x2,y2),radius=20,fill='white',outline='#81b5aa',width=3)
        d.text((x1+22,y1+19),label,fill='#174d44',font=f(29))
        for i,line in enumerate(body.split('\n')):d.text((x1+22,y1+70+i*37),line,fill='#48605e',font=f(24))
    for a,b,label in arrows:
        d.line((a,b),fill='#358a7e',width=5)
        import math
        angle=math.atan2(b[1]-a[1],b[0]-a[0]);length=15
        d.polygon([b,(b[0]-length*math.cos(angle-.45),b[1]-length*math.sin(angle-.45)),(b[0]-length*math.cos(angle+.45),b[1]-length*math.sin(angle+.45))],fill='#358a7e')
        if label:d.text(((a[0]+b[0])/2+8,(a[1]+b[1])/2-35),label,fill='#24675e',font=f(22))
    d.text((55,size[1]-62),footer,fill='#647575',font=f(23))
    im.save(assets/name)

figure('architecture.png','系统架构：采集、云端展示与数据库归档',[
    (45,165,360,385,'传感器与本地显示','AHT20：温度 / 湿度\nBH1750：光照\nSSD1306：OLED'),
    (465,165,790,385,'ESP32-S3 固件','GPIO8 / GPIO9 共享 I²C\nFreeRTOS 采集任务\nWi-Fi + MQTT 发布'),
    (930,165,1510,385,'新大陆云 NLECloud','Temperature / Humidity / Light\nMQTT 接收设备数据\nHTTPS API 提供查询'),
    (825,535,1150,735,'Android APP','HTTPS 查询最新值\n本次运行内存历史\n首页 / 历史 / 设备'),
    (1230,535,1560,735,'NAS 数据归档','HTTPS 读取历史默认页\n三指标同时间记录对齐\nPostgreSQL 去重写入'),
], [((360,270),(465,270),'I²C'),((790,270),(930,270),'MQTT'),((1030,385),(990,535),'HTTPS'),((1350,385),(1390,535),'HTTPS')],
'依据源码绘制的架构示意：Android 直接查询云平台，不读取 NAS 数据库。')

figure('data-boundaries.png','两条读取链路使用不同的时间规则',[
    (70,150,740,370,'Android 最新传感器快照','按 ApiTag 映射三个指标\n各自 RecordTime 可不同 → 采用最早时间\n缺字段 / 非法数值 / 非法时间 → 拒绝快照'),
    (860,150,1530,370,'NAS 历史记录归档','按三个指标的 RecordTime 字符串取交集\n只写同一时间均有三个数值的完整记录\n默认页以外的数据不自动补采'),
    (70,500,740,720,'APP 进程内历史','重复时间替换；旧快照不覆盖最新值\n保留近 7 天且最多 2000 个点\n重启后重新积累，不等于云端 7 天历史'),
    (860,500,1530,720,'PostgreSQL 持久化','sensor_data 保存温湿度、光照与时间\n(device_id, recorded_at) 唯一索引\n重复写入 ON CONFLICT DO NOTHING'),
], [((405,370),(405,500),''),((1195,370),(1195,500),'')],
'云端查询成功表示本次同步成功；当前 APP 响应中没有设备在线字段。')
