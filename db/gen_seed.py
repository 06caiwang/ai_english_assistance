"""生成 db/seed.sql（200 个单词 + 6 个形象）。

用脚本生成而不是手写 SQL，是为了以后想换词库时只改数据行。
运行：python db/gen_seed.py
"""
import io
import os

WORDS_PART1 = """
abandon|/əˈbændən/|v. 放弃；抛弃|He abandoned the plan at last.
ability|/əˈbɪləti/|n. 能力；才能|She has the ability to lead.
absorb|/əbˈsɔːb/|v. 吸收；吸引|Plants absorb water from soil.
abstract|/ˈæbstrækt/|adj. 抽象的 n. 摘要|This is an abstract idea.
academic|/ˌækəˈdemɪk/|adj. 学术的 n. 学者|His academic record is excellent.
accelerate|/əkˈseləreɪt/|v. 加速；促进|The car accelerated suddenly.
accept|/əkˈsept/|v. 接受；承认|I accept your apology.
access|/ˈækses/|n. 通道 v. 访问|Students have access to the library.
accident|/ˈæksɪdənt/|n. 事故；意外|The accident happened at noon.
accompany|/əˈkʌmpəni/|v. 陪伴；伴随|She accompanied me to the station.
accomplish|/əˈkʌmplɪʃ/|v. 完成；实现|We accomplished the task on time.
account|/əˈkaʊnt/|n. 账户；描述|Please check your bank account.
accurate|/ˈækjərət/|adj. 准确的|The data is accurate enough.
accuse|/əˈkjuːz/|v. 指责；控告|They accused him of lying.
achieve|/əˈtʃiːv/|v. 达到；实现|He achieved his goal finally.
acquire|/əˈkwaɪə(r)/|v. 获得；学到|Children acquire language quickly.
adapt|/əˈdæpt/|v. 适应；改编|You must adapt to the new rules.
adequate|/ˈædɪkwət/|adj. 足够的；适当的|We need adequate time to prepare.
adjust|/əˈdʒʌst/|v. 调整；适应|Please adjust the volume.
admire|/ədˈmaɪə(r)/|v. 钦佩；欣赏|I admire her courage.
admit|/ədˈmɪt/|v. 承认；准许进入|He admitted his mistake.
adopt|/əˈdɒpt/|v. 采用；收养|They adopted a new method.
advance|/ədˈvɑːns/|v. 前进 n. 进步|Science advances year by year.
advantage|/ədˈvɑːntɪdʒ/|n. 优势；好处|Speed is our main advantage.
adventure|/ədˈventʃə(r)/|n. 冒险；奇遇|The trip was a real adventure.
advertise|/ˈædvətaɪz/|v. 做广告|They advertise the product online.
affect|/əˈfekt/|v. 影响；感动|Weather affects my mood.
afford|/əˈfɔːd/|v. 负担得起|I cannot afford a new car.
afraid|/əˈfreɪd/|adj. 害怕的|She is afraid of dogs.
agency|/ˈeɪdʒənsi/|n. 代理机构|He works for a travel agency.
agenda|/əˈdʒendə/|n. 议程|The agenda was sent by email.
aggressive|/əˈɡresɪv/|adj. 好斗的；积极的|His style is too aggressive.
agree|/əˈɡriː/|v. 同意；一致|We agree with your plan.
agriculture|/ˈæɡrɪkʌltʃə(r)/|n. 农业|Agriculture is vital to the region.
aim|/eɪm/|n. 目标 v. 瞄准|Our aim is to finish early.
alarm|/əˈlɑːm/|n. 警报 v. 使惊恐|The fire alarm rang loudly.
album|/ˈælbəm/|n. 相册；专辑|She bought a new album.
alcohol|/ˈælkəhɒl/|n. 酒精|Alcohol is not allowed here.
alert|/əˈlɜːt/|adj. 警觉的 v. 提醒|Stay alert during the test.
alive|/əˈlaɪv/|adj. 活着的|The old tree is still alive.
allow|/əˈlaʊ/|v. 允许；给予|Smoking is not allowed.
alter|/ˈɔːltə(r)/|v. 改变；修改|We altered the schedule.
alternative|/ɔːlˈtɜːnətɪv/|n. 替代方案 adj. 可选的|We need an alternative plan.
although|/ɔːlˈðəʊ/|conj. 虽然|Although it rained, we went out.
altitude|/ˈæltɪtjuːd/|n. 高度；海拔|The altitude made breathing hard.
amount|/əˈmaʊnt/|n. 数量 v. 总计|A large amount of water was wasted.
analyse|/ˈænəlaɪz/|v. 分析|Let us analyse the results.
ancient|/ˈeɪnʃənt/|adj. 古代的|They studied ancient history.
anger|/ˈæŋɡə(r)/|n. 愤怒 v. 激怒|He could not hide his anger.
announce|/əˈnaʊns/|v. 宣布|They announced the winner.
annual|/ˈænjuəl/|adj. 每年的 n. 年刊|The annual report is ready.
anxious|/ˈæŋkʃəs/|adj. 焦虑的；渴望的|She is anxious about the exam.
apologize|/əˈpɒlədʒaɪz/|v. 道歉|He apologized for being late.
apparent|/əˈpærənt/|adj. 明显的|The reason is apparent to all.
appeal|/əˈpiːl/|v. 呼吁；吸引 n. 吸引力|The design appeals to young people.
appear|/əˈpɪə(r)/|v. 出现；似乎|A rainbow appeared after rain.
appetite|/ˈæpɪtaɪt/|n. 食欲；欲望|Exercise gave me a good appetite.
applaud|/əˈplɔːd/|v. 鼓掌；称赞|The audience applauded loudly.
apply|/əˈplaɪ/|v. 申请；应用|She applied for a scholarship.
appoint|/əˈpɔɪnt/|v. 任命；约定|They appointed him manager.
appreciate|/əˈpriːʃieɪt/|v. 欣赏；感激|I appreciate your help.
approach|/əˈprəʊtʃ/|n. 方法 v. 接近|We tried a new approach.
appropriate|/əˈprəʊpriət/|adj. 合适的|Wear appropriate clothes.
approve|/əˈpruːv/|v. 批准；赞成|The board approved the budget.
arise|/əˈraɪz/|v. 出现；产生|Problems arise from poor planning.
arrange|/əˈreɪndʒ/|v. 安排；整理|Let us arrange a meeting.
arrest|/əˈrest/|v. 逮捕 n. 逮捕|The police arrested two men.
arrive|/əˈraɪv/|v. 到达|We arrived at the hotel late.
artificial|/ˌɑːtɪˈfɪʃl/|adj. 人造的；虚假的|Artificial flowers look real.
aspect|/ˈæspekt/|n. 方面；外观|Consider every aspect of the issue.
assess|/əˈses/|v. 评估|Teachers assess students monthly.
assign|/əˈsaɪn/|v. 分配；指派|They assigned the task to me.
assist|/əˈsɪst/|v. 帮助|Two nurses assisted the doctor.
associate|/əˈsəʊʃieɪt/|v. 联想；交往 n. 同事|We associate red with danger.
assume|/əˈsjuːm/|v. 假设；承担|Do not assume he agrees.
assure|/əˈʃʊə(r)/|v. 保证；使确信|I assure you it is safe.
atmosphere|/ˈætməsfɪə(r)/|n. 大气；气氛|The atmosphere here is friendly.
attach|/əˈtætʃ/|v. 附上；连接|Please attach the file.
attempt|/əˈtempt/|v. 尝试 n. 企图|He attempted the exam twice.
attend|/əˈtend/|v. 参加；照料|She attends class every day.
attitude|/ˈætɪtjuːd/|n. 态度|A positive attitude helps a lot.
attract|/əˈtrækt/|v. 吸引|The show attracted many visitors.
audience|/ˈɔːdiəns/|n. 观众；听众|The audience loved the song.
authority|/ɔːˈθɒrəti/|n. 权威；当局|The local authority approved it.
available|/əˈveɪləbl/|adj. 可用的；空闲的|The room is available now.
average|/ˈævərɪdʒ/|adj. 平均的 n. 平均数|His score is above average.
avoid|/əˈvɔɪd/|v. 避免|Avoid heavy meals at night.
await|/əˈweɪt/|v. 等待|We await your reply.
awake|/əˈweɪk/|adj. 醒着的 v. 唤醒|She stayed awake all night.
award|/əˈwɔːd/|n. 奖项 v. 授予|He won an award for writing.
aware|/əˈweə(r)/|adj. 意识到的|Are you aware of the risk?
balance|/ˈbæləns/|n. 平衡 v. 使平衡|Keep a balance between work and rest.
ban|/bæn/|v. 禁止 n. 禁令|The city banned smoking indoors.
bargain|/ˈbɑːɡən/|n. 便宜货 v. 讨价还价|This coat is a real bargain.
barrier|/ˈbæriə(r)/|n. 障碍；屏障|Language is no barrier here.
basis|/ˈbeɪsɪs/|n. 基础；依据|Trust is the basis of friendship.
battle|/ˈbætl/|n. 战斗 v. 斗争|They battled against the flood.
bear|/beə(r)/|v. 忍受；承担 n. 熊|I cannot bear the noise.
behalf|/bɪˈhɑːf/|n. 代表（on behalf of）|She spoke on behalf of the class.
"""

WORDS_PART2 = """
benefit|/ˈbenɪfɪt/|n. 好处 v. 有益于|Regular exercise benefits health.
blame|/bleɪm/|v. 责备 n. 责任|Do not blame others for this.
blank|/blæŋk/|adj. 空白的 n. 空白|Fill in the blank carefully.
boost|/buːst/|v. 提高；促进|This news boosted our confidence.
border|/ˈbɔːdə(r)/|n. 边界 v. 接壤|The river forms the border.
borrow|/ˈbɒrəʊ/|v. 借入|May I borrow your pen?
bother|/ˈbɒðə(r)/|v. 打扰；费心|Sorry to bother you so late.
brief|/briːf/|adj. 简短的 n. 摘要|Please give a brief answer.
brilliant|/ˈbrɪliənt/|adj. 杰出的；明亮的|That was a brilliant idea.
budget|/ˈbʌdʒɪt/|n. 预算 v. 编预算|The budget is rather tight.
burden|/ˈbɜːdn/|n. 负担 v. 使负担|Heavy taxes burden the poor.
burst|/bɜːst/|v. 爆裂；突然发生|The balloon burst suddenly.
calculate|/ˈkælkjuleɪt/|v. 计算；估计|Please calculate the total cost.
campaign|/kæmˈpeɪn/|n. 运动；战役|They started a reading campaign.
cancel|/ˈkænsl/|v. 取消|The flight was cancelled.
capable|/ˈkeɪpəbl/|adj. 有能力的|She is capable of better work.
capital|/ˈkæpɪtl/|n. 首都；资本|Beijing is the capital of China.
career|/kəˈrɪə(r)/|n. 职业；生涯|He started his career as a teacher.
casual|/ˈkæʒuəl/|adj. 随意的；偶然的|Casual clothes are fine here.
category|/ˈkætəɡəri/|n. 种类；类别|Sort the books by category.
cease|/siːs/|v. 停止|The rain finally ceased.
celebrate|/ˈselɪbreɪt/|v. 庆祝|We celebrated her birthday.
challenge|/ˈtʃælɪndʒ/|n. 挑战 v. 质疑|Learning English is a challenge.
channel|/ˈtʃænl/|n. 频道；渠道|The news is on channel five.
chapter|/ˈtʃæptə(r)/|n. 章节|Read the first chapter tonight.
character|/ˈkærəktə(r)/|n. 性格；角色|The main character is a girl.
charge|/tʃɑːdʒ/|v. 收费；指控 n. 费用|They charge ten yuan an hour.
chart|/tʃɑːt/|n. 图表 v. 记录|The chart shows the trend.
cheer|/tʃɪə(r)/|v. 欢呼；鼓励|The crowd cheered loudly.
chemical|/ˈkemɪkl/|adj. 化学的 n. 化学品|Avoid harsh chemicals on skin.
circumstance|/ˈsɜːkəmstəns/|n. 情况；环境|Under no circumstance give up.
civil|/ˈsɪvl/|adj. 公民的；民事的|Civil rights matter to everyone.
claim|/kleɪm/|v. 声称；索取 n. 主张|He claims to know the answer.
clarify|/ˈklærəfaɪ/|v. 澄清；阐明|Could you clarify that point?
classic|/ˈklæsɪk/|adj. 经典的 n. 名著|This is a classic novel.
climate|/ˈklaɪmət/|n. 气候|The climate here is mild.
clothing|/ˈkləʊðɪŋ/|n. 衣物|Warm clothing is necessary.
collapse|/kəˈlæps/|v. 倒塌；崩溃|The old bridge collapsed.
colleague|/ˈkɒliːɡ/|n. 同事|My colleague helped me a lot.
collect|/kəˈlekt/|v. 收集；领取|He collects old coins.
combine|/kəmˈbaɪn/|v. 结合；合并|Combine the two lists.
comfort|/ˈkʌmfət/|n. 舒适 v. 安慰|Her words brought me comfort.
command|/kəˈmɑːnd/|n. 命令 v. 指挥|He commands great respect.
comment|/ˈkɒment/|n. 评论 v. 评论|Please leave a comment below.
commercial|/kəˈmɜːʃl/|adj. 商业的 n. 广告|The commercial was too long.
commit|/kəˈmɪt/|v. 犯（错）；承诺|He committed a small error.
common|/ˈkɒmən/|adj. 常见的；共同的|This mistake is very common.
compare|/kəmˈpeə(r)/|v. 比较|Compare the two answers.
compete|/kəmˈpiːt/|v. 竞争|Ten teams competed for the prize.
complain|/kəmˈpleɪn/|v. 抱怨|Stop complaining and try again.
complete|/kəmˈpliːt/|v. 完成 adj. 完整的|Complete the form in English.
complex|/ˈkɒmpleks/|adj. 复杂的 n. 综合体|The problem is quite complex.
concentrate|/ˈkɒnsntreɪt/|v. 集中；专心|Concentrate on one task at a time.
concept|/ˈkɒnsept/|n. 概念；观念|The concept is easy to grasp.
concern|/kənˈsɜːn/|n. 关心；担忧 v. 涉及|Safety is our main concern.
conclude|/kənˈkluːd/|v. 得出结论；结束|We concluded that he was right.
condition|/kənˈdɪʃn/|n. 条件；状况|The car is in good condition.
conduct|/kənˈdʌkt/|v. 进行；指挥 n. 行为|They conducted a survey.
confidence|/ˈkɒnfɪdəns/|n. 信心；信任|Practice builds confidence.
confirm|/kənˈfɜːm/|v. 确认；证实|Please confirm your booking.
conflict|/ˈkɒnflɪkt/|n. 冲突 v. 抵触|Their views are in conflict.
confuse|/kənˈfjuːz/|v. 使困惑；混淆|Do not confuse these two words.
connect|/kəˈnekt/|v. 连接；联系|Connect the phone to wifi.
conscious|/ˈkɒnʃəs/|adj. 有意识的；察觉的|She was conscious of the risk.
consequence|/ˈkɒnsɪkwəns/|n. 后果；结果|Think about the consequences.
consider|/kənˈsɪdə(r)/|v. 考虑；认为|We are considering your proposal.
consist|/kənˈsɪst/|v. 由…组成|The team consists of six people.
constant|/ˈkɒnstənt/|adj. 持续的；恒定的|Keep the temperature constant.
construct|/kənˈstrʌkt/|v. 建造；构建|They constructed a new bridge.
consult|/kənˈsʌlt/|v. 咨询；查阅|Consult a doctor if it hurts.
consume|/kənˈsjuːm/|v. 消耗；消费|This machine consumes little power.
contain|/kənˈteɪn/|v. 包含；容纳|The box contains old letters.
contemporary|/kənˈtemprəri/|adj. 当代的；同时代的|She studies contemporary art.
content|/ˈkɒntent/|n. 内容 adj. 满足的|The content of the course is rich.
context|/ˈkɒntekst/|n. 上下文；背景|Guess the meaning from context.
continue|/kənˈtɪnjuː/|v. 继续|Please continue your story.
contract|/ˈkɒntrækt/|n. 合同 v. 收缩|They signed a one-year contract.
contrast|/ˈkɒntrɑːst/|n. 对比 v. 形成对照|The contrast is quite clear.
contribute|/kənˈtrɪbjuːt/|v. 贡献；促成|Everyone contributed some ideas.
control|/kənˈtrəʊl/|v. 控制 n. 控制|Keep your temper under control.
convenient|/kənˈviːniənt/|adj. 方便的|Come at any convenient time.
convince|/kənˈvɪns/|v. 使信服；说服|He convinced me to join.
cooperate|/kəʊˈɒpəreɪt/|v. 合作|The two sides agreed to cooperate.
correct|/kəˈrekt/|adj. 正确的 v. 改正|Correct the mistakes in red.
cost|/kɒst/|n. 成本 v. 花费|The cost is higher than expected.
courage|/ˈkʌrɪdʒ/|n. 勇气|It takes courage to say no.
create|/kriˈeɪt/|v. 创造；造成|The app creates a new habit.
credit|/ˈkredɪt/|n. 信用；学分 v. 归功于|He got credit for the work.
crisis|/ˈkraɪsɪs/|n. 危机|The company survived the crisis.
critical|/ˈkrɪtɪkl/|adj. 关键的；批判的|Timing is critical here.
criticize|/ˈkrɪtɪsaɪz/|v. 批评|Do not criticize before you try.
crowd|/kraʊd/|n. 人群 v. 挤满|A crowd gathered at the gate.
crucial|/ˈkruːʃl/|adj. 至关重要的|Sleep is crucial for memory.
cultural|/ˈkʌltʃərəl/|adj. 文化的|They respect cultural differences.
curious|/ˈkjʊəriəs/|adj. 好奇的|Children are curious about everything.
current|/ˈkʌrənt/|adj. 当前的 n. 电流|The current situation is stable.
custom|/ˈkʌstəm/|n. 习俗；惯例|It is a local custom here.
damage|/ˈdæmɪdʒ/|n. 损害 v. 损坏|The storm caused great damage.
danger|/ˈdeɪndʒə(r)/|n. 危险|The bridge is in danger.
debate|/dɪˈbeɪt/|n. 辩论 v. 讨论|They debated the topic for hours.
declare|/dɪˈkleə(r)/|v. 宣布；申报|He declared his intention clearly.
"""

CHARACTERS = [
    ("熊猫", "char_panda", 0),
    ("鸭子", "char_duck", 50),
    ("青蛙", "char_frog", 100),
    ("企鹅", "char_penguin", 100),
    ("小熊", "char_bear", 200),
    ("僵尸", "char_zombie", 200),
]


def sql_escape(s: str) -> str:
    return s.replace("\\", "\\\\").replace("'", "''")


def main() -> None:
    words = []
    for block in (WORDS_PART1, WORDS_PART2):
        for line in block.strip().splitlines():
            line = line.strip()
            if not line:
                continue
            parts = line.split("|")
            if len(parts) != 4:
                raise ValueError(f"格式错误（应为 4 段）：{line}")
            words.append(parts)

    spellings = [w[0] for w in words]
    dup = {s for s in spellings if spellings.count(s) > 1}
    if dup:
        raise ValueError(f"存在重复单词：{dup}")

    out = io.StringIO()
    out.write("-- 由 db/gen_seed.py 生成，勿手改\n")
    out.write("-- 200 个单词 + 6 个形象\n\n")
    out.write("SET NAMES utf8mb4;\n\n")
    out.write("DELETE FROM `character`;\n")
    out.write("DELETE FROM `word`;\n\n")

    out.write("INSERT INTO `word` (`spelling`, `phonetic`, `meaning`, `example`) VALUES\n")
    rows = [
        "('{}', '{}', '{}', '{}')".format(*(sql_escape(x) for x in w)) for w in words
    ]
    out.write(",\n".join(rows))
    out.write(";\n\n")

    out.write("INSERT INTO `character` (`name`, `image`, `price`) VALUES\n")
    crows = ["('{}', '{}', {})".format(sql_escape(name), sql_escape(image), price)
             for name, image, price in CHARACTERS]
    out.write(",\n".join(crows))
    out.write(";\n")

    path = os.path.join(os.path.dirname(os.path.abspath(__file__)), "seed.sql")
    with open(path, "w", encoding="utf-8") as f:
        f.write(out.getvalue())

    print(f"已生成 {path}")
    print(f"单词 {len(words)} 条，形象 {len(CHARACTERS)} 个")


if __name__ == "__main__":
    main()
