package com.jiankang.app.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate

private data class DailyQuote(val text: String, val attribution: String)

private val quotes = listOf(
    DailyQuote("不以物喜，不以己悲。", "范仲淹《岳阳楼记》"),
    DailyQuote("知足者富。", "《道德经》第三十三章"),
    DailyQuote("君子坦荡荡，小人长戚戚。", "《论语·述而》"),
    DailyQuote("天行健，君子以自强不息。", "《周易·乾》"),
    DailyQuote("流水不腐，户枢不蠹。", "《吕氏春秋·尽数》"),
    DailyQuote("岁寒，然后知松柏之后凋也。", "《论语·子罕》"),
    DailyQuote("三人行，必有我师焉。", "《论语·述而》")
)

private data class HealthEvidence(
    val title: String,
    val lead: String,
    val prevention: String,
    val screening: String,
    val treatment: String,
    val source: String,
    val sourceUrl: String
)

private val evidences = listOf(
    HealthEvidence(
        "心血管健康：八项生活指标",
        "健康饮食、身体活动、远离烟草、充足睡眠，以及体重、血脂、血糖、血压管理，共同影响长期心血管健康。",
        "优先选蔬菜、水果、全谷物和优质蛋白；合理安排运动与睡眠，减少吸烟及二手烟暴露。",
        "定期了解血压、血糖、血脂；是否需要进一步检查应结合年龄、家族史和医生意见。",
        "已确诊高血压、糖尿病或血脂异常者，应根据医疗建议管理风险因素和规范治疗，勿自行停药。",
        "美国心脏协会 · Life's Essential 8",
        "https://www.heart.org/en/healthy-living/healthy-lifestyle/lifes-essential-8"
    ),
    HealthEvidence(
        "心脑血管病预防：盐与血压",
        "高血压、血糖和血脂异常、肥胖、烟草以及缺少运动均与心脑血管风险有关。",
        "减少过量食盐和高盐加工食品，增加蔬果与活动；避免吸烟、尽量不饮酒。",
        "家庭血压可按规范方法记录多次读数；连续异常时向医疗人员咨询，不只凭一次测量判断。",
        "需要治疗时，通常包括生活方式调整，必要时在医生评估下使用药物控制血压和其他风险。",
        "世界卫生组织 · 心血管疾病概况（2025）",
        "https://www.who.int/news-room/fact-sheets/detail/cardiovascular-diseases-%28cvds%29"
    ),
    HealthEvidence(
        "癌症预防：关注可改变风险",
        "许多癌症风险与吸烟、饮酒、体重、活动不足、某些感染和环境暴露有关。",
        "不吸烟、尽量不饮酒、保持运动与健康饮食；符合条件者按建议接种乙肝和人乳头瘤病毒疫苗，并做好防晒。",
        "癌症筛查的种类与起始年龄应依据性别、年龄、家族史和所在地指南决定，避免不必要检查。",
        "如筛查或症状提示异常，需要及时进行规范诊断。治疗方案依癌种、分期和个体情况选择。",
        "世界卫生组织 · Cancer（2026）",
        "https://www.who.int/news-room/fact-sheets/detail/cancer"
    ),
    HealthEvidence(
        "卒中预警：识别突发症状",
        "突发口角歪斜、一侧肢体无力、说话含糊、看不清或失去平衡，可能是卒中信号。",
        "积极管理血压和其他危险因素；规律运动、健康饮食并远离烟草。",
        "上述症状即使短暂消失，也应立即寻求急诊评估。记下首次出现症状的时间。",
        "一旦怀疑卒中，请立即拨打当地急救电话。治疗有严格时间窗，不应自行服药或等待。",
        "美国疾病控制与预防中心 · Stroke Signs and Symptoms（2026）",
        "https://www.cdc.gov/stroke/signs-symptoms/"
    ),
    HealthEvidence(
        "癌症早诊与规范治疗",
        "发现得早、诊断准确并接受适当治疗，部分癌症具有较高治愈可能。",
        "降低吸烟、饮酒等危险因素，参与适合自己的规范筛查；症状持续或出现新的异常时及时就诊。",
        "筛查异常并不等于确诊癌症，需医生结合影像、病理等检查判定。",
        "治疗可能包含手术、放疗、药物等，按具体癌种、分期与个体状况制定；不要自行购买所谓抗癌药物。",
        "世界卫生组织 · Cancer: Treatment（2026）",
        "https://www.who.int/news-room/fact-sheets/detail/cancer"
    ),
    HealthEvidence(
        "健康睡眠与心脏保护",
        "规律睡眠是长期健康管理的重要组成部分，多数成年人推荐7—9小时睡眠。",
        "保持较稳定的睡眠时段、白天适当活动，睡前减少刺激性内容和不必要的刷屏。",
        "长期严重失眠、明显白天困倦或睡眠中疑似呼吸暂停，可考虑专业评估。",
        "慢性睡眠问题需要明确原因，治疗方式应由专业人员根据情况选择。",
        "美国心脏协会 · Life's Essential 8",
        "https://www.heart.org/en/healthy-living/healthy-lifestyle/lifes-essential-8"
    )
)

private fun quoteFor(date: LocalDate): DailyQuote =
    quotes[Math.floorMod(date.toEpochDay(), quotes.size.toLong()).toInt()]

private fun tipFor(date: LocalDate): HealthEvidence =
    evidences[Math.floorMod(date.toEpochDay(), evidences.size.toLong()).toInt()]

@Composable
fun DailyQuoteCard(date: LocalDate) {
    val quote = quoteFor(date)
    AppCard(background = Color(0xFFFFF7E9)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.WbSunny, null, tint = Orange)
            Spacer(Modifier.width(9.dp))
            Text("每日一句", fontWeight = FontWeight.Bold, color = Ink)
        }
        Spacer(Modifier.height(10.dp))
        Text("“" + quote.text + "”", fontSize = 15.sp, color = Ink, lineHeight = 22.sp)
        Spacer(Modifier.height(6.dp))
        Text("—— " + quote.attribution, fontSize = 11.sp, color = Slate)
    }
}

@Composable
fun DailyHealthCard(date: LocalDate, onOpen: () -> Unit) {
    val tip = tipFor(date)
    AppCard(modifier = Modifier.clickable(onClick = onOpen)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ColorIcon(Icons.Default.Favorite, Coral)
            Spacer(Modifier.width(11.dp))
            Text("今日健康知识", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Icon(Icons.Default.ChevronRight, null, tint = Slate)
        }
        Spacer(Modifier.height(10.dp))
        Text(tip.title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = DarkGreen)
        Spacer(Modifier.height(6.dp))
        Text(tip.lead, fontSize = 12.sp, lineHeight = 18.sp, color = Slate, maxLines = 4)
        Spacer(Modifier.height(7.dp))
        SmallLabel("来源：" + tip.source)
    }
}

@Composable
fun HealthGuideScreen(date: LocalDate, onBack: () -> Unit) {
    val context = LocalContext.current
    val todaysTip = tipFor(date)
    PageColumn("健康研究与疾病预防", date, onBack = onBack) {
        Spacer(Modifier.height(8.dp))
        AppCard(background = SoftGreen) {
            SectionHeading("今日重点")
            Spacer(Modifier.height(8.dp))
            Text(todaysTip.title, color = DarkGreen, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(7.dp))
            Text(todaysTip.lead, color = Ink, lineHeight = 20.sp, fontSize = 13.sp)
        }
        evidences.forEach { tip ->
            AppCard {
                SectionHeading(tip.title)
                Spacer(Modifier.height(9.dp))
                EvidenceLine("研究要点", tip.lead)
                EvidenceLine("预防", tip.prevention)
                EvidenceLine("筛查与早诊", tip.screening)
                EvidenceLine("治疗或就医", tip.treatment)
                HorizontalDivider(modifier = Modifier.padding(vertical = 9.dp), color = Line)
                TextButton(onClick = {
                    try {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(tip.sourceUrl)))
                    } catch (_: Exception) { showMessage(context, "无法打开浏览器") }
                }) {
                    Icon(Icons.Default.OpenInNew, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(5.dp))
                    Text(tip.source, fontSize = 12.sp)
                }
            }
        }
        SmallLabel("内容为离线整理的官方公开资料，设定时间：2026年10月；不会自动在线更新。不能代替个人诊断或用药指导。出现胸痛、突发神经功能障碍等急症请及时呼叫当地急救电话。")
    }
}

@Composable
private fun EvidenceLine(title: String, body: String) {
    Text(title, fontWeight = FontWeight.SemiBold, color = DarkGreen, fontSize = 12.sp)
    Spacer(Modifier.height(3.dp))
    Text(body, color = Ink, fontSize = 12.sp, lineHeight = 18.sp)
    Spacer(Modifier.height(10.dp))
}
