package com.example.data

data class Verse(
    val verseNumber: Int,
    val arabic: String,
    val translation: String
)

data class Surah(
    val number: Int,
    val name: String,
    val englishName: String,
    val englishMeaning: String,
    val type: String, // Meccan or Medinan
    val verses: List<Verse>
)

object QuranData {
    val surahs = listOf(
        Surah(
            number = 1,
            name = "الفاتحة",
            englishName = "Al-Fatihah",
            englishMeaning = "The Opening",
            type = "Meccan",
            verses = listOf(
                Verse(1, "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ", "In the name of Allah, the Entirely Merciful, the Especially Merciful."),
                Verse(2, "الْحَمْدُ لِلَّهِ رَبِّ الْعَالَمِينَ", "[All] praise is [due] to Allah, Lord of the worlds -"),
                Verse(3, "الرَّحْمَٰنِ الرَّحِيمِ", "The Entirely Merciful, the Especially Merciful,"),
                Verse(4, "مَالِكِ يَوْمِ الدِّينِ", "Sovereign of the Day of Recompense."),
                Verse(5, "إِيَّاكَ نَعْبُدُ وَإِيَّاكَ نَسْتَعِينُ", "It is You we worship and You we ask for help."),
                Verse(6, "اهْدِنَا الصِّرَاطَ الْمُسْتَقِيمَ", "Guide us to the straight path -"),
                Verse(7, "صِرَاطَ الَّذِينَ أَنْعَمْتَ عَلَيْهِمْ غَيْرِ الْمَغْضُوبِ عَلَيْهِمْ وَلَا الضَّالِّينَ", "The path of those upon whom You have bestowed favor, not of those who have evoked [Your] anger or of those who are astray.")
            )
        ),
        Surah(
            number = 18,
            name = "الكهف",
            englishName = "Al-Kahf",
            englishMeaning = "The Cave",
            type = "Meccan",
            verses = listOf(
                Verse(1, "الْحَمْدُ لِلَّهِ الَّذِي أَنزَلَ عَلَىٰ عَبْدِهِ الْكِتَابَ وَلَمْ يَجْعَل لَّهُ عِوَجًا", "[All] praise is [due] to Allah, who has sent down upon His Servant the Book and has not made therein any deviance."),
                Verse(2, "قَيِّمًا لِّيُنذِرَ بَأْسًا شَدِيدًا مِّن لَّدُنْهُ وَيُبَشِّرَ الْمُؤْمِنِينَ الَّذِينَ يَعْمَلُونَ الصَّالِحَاتِ أَنَّ لَهُمْ أَجْرًا حَسَنًا", "[He has made it] straight, to warn of severe punishment from Him and to give good tidings to the believers who do righteous deeds that they will have a good reward,"),
                Verse(3, "مَّاكِثِينَ فِيهِ أَبَدًا", "In which they will remain forever"),
                Verse(4, "وَيُنذِرَ الَّذِينَ قَالُوا اتَّخَذَ اللَّهُ وَلَدًا", "And to warn those who say, 'Allah has taken a son.'"),
                Verse(5, "مَّا لَهُم بِهِ مِنْ عِلْمٍ وَلَا لِآبَائِهِمْ ۚ كَبُرَتْ كَلِمَةً تَخْرُجُ مِنْ أَفْوَاهِهِمْ ۚ إِن يَقُولُونَ إِلَّا كَذِبًا", "They have no knowledge of it, nor had their fathers. Grave is the word that comes out of their mouths; they speak not except a lie.")
            )
        ),
        Surah(
            number = 36,
            name = "يس",
            englishName = "Ya-Sin",
            englishMeaning = "Ya-Sin",
            type = "Meccan",
            verses = listOf(
                Verse(1, "يس", "Ya, Seen."),
                Verse(2, "وَالْقُرْآنِ الْحَكِيمِ", "By the wise Qur'an,"),
                Verse(3, "إِنَّكَ لَمِنَ الْمُرْسَلِينَ", "Indeed you, [O Muhammad], are from among the messengers,"),
                Verse(4, "عَلَىٰ صِرَاطٍ مُّسْتَقِيمٍ", "On a straight path."),
                Verse(5, "تَنزِيلَ الْعَزِيزِ الرَّحِيمِ", "[This is] a revelation of the Exalted in Might, the Merciful,")
            )
        ),
        Surah(
            number = 55,
            name = "الرحمن",
            englishName = "Ar-Rahman",
            englishMeaning = "The Beneficent",
            type = "Medinan",
            verses = listOf(
                Verse(1, "الرَّحْمَٰنُ", "The Most Merciful"),
                Verse(2, "عَلَّمَ الْقُرْآنَ", "Taught the Qur'an,"),
                Verse(3, "خَلَقَ الْإِنسَانَ", "Created man,"),
                Verse(4, "عَلَّمَهُ الْبَيَانَ", "Taught him eloquence."),
                Verse(5, "الشَّمْسُ وَالْقَمَرُ بِحُسْبَانٍ", "The sun and the moon [move] by precise calculation,")
            )
        ),
        Surah(
            number = 67,
            name = "الملك",
            englishName = "Al-Mulk",
            englishMeaning = "The Sovereignty",
            type = "Meccan",
            verses = listOf(
                Verse(1, "تَبَارَكَ الَّذِي بِيَدِهِ الْمُلْكُ وَهُوَ عَلَىٰ كُلِّ شَيْءٍ قَدِيرٌ", "Blessed is He in whose hand is dominion, and He is over all things competent -"),
                Verse(2, "الَّذِي خَلَقَ الْمَوْتَ وَالْحَيَاةَ لِيَبْلُوَكُمْ أَيُّكُمْ أَحْسَنُ عَمَلًا ۚ وَهُوَ الْعَزِيزُ الْغَفُورُ", "[He] who created death and life to test you [as to] which of you is best in deed - and He is the Exalted in Might, the Forgiving -"),
                Verse(3, "الَّذِي خَلَقَ سَبْعَ سَمَاوَاتٍ طِبَاقًا ۖ مَّا تَرَىٰ فِي خَلْقِ الرَّحْمَٰنِ مِن تَفَاوُتٍ ۖ فَارْجِعِ الْبَصَرَ هَلْ تَرَىٰ مِن فُطُورٍ", "[And] who created seven heavens in layers. You do not see in the creation of the Most Merciful any inconsistency. So return [your] vision [to the sky]; do you see any breaks?")
            )
        ),
        Surah(
            number = 112,
            name = "الإخلاص",
            englishName = "Al-Ikhlas",
            englishMeaning = "The Sincerity",
            type = "Meccan",
            verses = listOf(
                Verse(1, "قُلْ هُوَ اللَّهُ أَحَدٌ", "Say, 'He is Allah, [who is] One,"),
                Verse(2, "اللَّهُ الصَّمَدُ", "Allah, the Eternal Refuge."),
                Verse(3, "لَمْ يَلِدْ وَلَمْ يُولَدْ", "He neither begets nor is born,"),
                Verse(4, "وَلَمْ يَكُن لَّهُ كُفُوًا أَحَدٌ", "Nor is there to Him any equivalent.'")
            )
        ),
        Surah(
            number = 113,
            name = "الفلق",
            englishName = "Al-Falaq",
            englishMeaning = "The Daybreak",
            type = "Meccan",
            verses = listOf(
                Verse(1, "قُلْ أَعُوذُ بِرَبِّ الْفَلَقِ", "Say, 'I seek refuge in the Lord of daybreak"),
                Verse(2, "مِن شَرِّ مَا خَلَقَ", "From the evil of that which He created"),
                Verse(3, "مِن شَرِّ غَاسِقٍ إِذَا وَقَبَ", "And from the evil of darkness when it settles"),
                Verse(4, "مِن شَرِّ النَّفَّاثَاتِ فِي الْعُقَدِ", "And from the evil of the blowers in knots"),
                Verse(5, "مِن شَرِّ حَاسِدٍ إِذَا حَسَدَ", "And from the evil of an envier when he envies.'")
            )
        ),
        Surah(
            number = 114,
            name = "الناس",
            englishName = "An-Nas",
            englishMeaning = "Mankind",
            type = "Meccan",
            verses = listOf(
                Verse(1, "قُلْ أَعُوذُ بِرَبِّ النَّاسِ", "Say, 'I seek refuge in the Lord of mankind,"),
                Verse(2, "مَلِكِ النَّاسِ", "The Sovereign of mankind,"),
                Verse(3, "إِلَٰهِ النَّاسِ", "The God of mankind,"),
                Verse(4, "مِن شَرِّ الْوَسْوَاسِ الْخَنَّاسِ", "From the evil of the retreating whisperer -"),
                Verse(5, "الَّذِي يُوَسْوِسُ فِي صُدُورِ النَّاسِ", "Who whispers [evil] into the breasts of mankind -"),
                Verse(6, "مِنَ الْجِنَّةِ وَالنَّاسِ", "From among the jinn and mankind.'")
            )
        )
    )
}
