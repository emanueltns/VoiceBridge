package freeapp.voicebridge.ai.domain.usecase

import javax.inject.Inject

class GetIdleEntertainmentUseCase @Inject constructor() {

    private val items = listOf(
        "Did you know? Honey never spoils. Archaeologists have found 3000-year-old honey in Egyptian tombs that was still perfectly edible.",
        "Here's a fun fact: Octopuses have three hearts and blue blood.",
        "Did you know? A group of flamingos is called a flamboyance.",
        "Fun fact: Bananas are berries, but strawberries aren't.",
        "Here's something interesting: The shortest war in history lasted 38 minutes, between Britain and Zanzibar.",
        "Did you know? There are more possible iterations of a game of chess than there are atoms in the known universe.",
        "Fun fact: A day on Venus is longer than a year on Venus.",
        "Here's a fun one: Cows have best friends and get stressed when separated.",
        "Did you know? The inventor of the Pringles can is buried in one.",
        "Fun fact: Wombat poop is cube-shaped.",
        "Here's something cool: Sea otters hold hands while sleeping so they don't drift apart.",
        "Did you know? The heart of a blue whale is so big that a small child could swim through its arteries.",
        "Fun fact: Scotland's national animal is the unicorn.",
        "Here's an interesting one: A bolt of lightning is five times hotter than the surface of the sun.",
        "Did you know? There's a species of jellyfish that is biologically immortal.",
        "Fun fact: The total weight of all ants on Earth roughly equals the total weight of all humans.",
        "Here's something fascinating: A teaspoonful of neutron star would weigh about 6 billion tons.",
        "Did you know? Sharks have been around longer than trees.",
        "Fun fact: The longest hiccupping spree lasted 68 years.",
        "Here's a wild one: There are more stars in the universe than grains of sand on all of Earth's beaches.",
        "Did you know? Cleopatra lived closer in time to the Moon landing than to the construction of the Great Pyramid.",
        "Fun fact: A cloud can weigh more than a million pounds.",
        "Here's something neat: Honey bees can recognize human faces.",
        "Did you know? The first oranges weren't orange. They were green.",
        "Fun fact: Your brain uses about 20 percent of your body's total energy.",
        "Here's an interesting thought: If you shuffled a deck of cards properly, the resulting order has likely never existed before in history.",
        "Did you know? Butterflies can taste with their feet.",
        "Fun fact: The Eiffel Tower can grow up to 6 inches taller during summer due to heat expansion.",
        "Here's a cool one: Dolphins have names for each other.",
        "Did you know? There's enough gold in Earth's core to coat the entire surface in a layer 1.5 feet thick.",
        "Fun fact: Astronauts grow up to 2 inches taller in space.",
        "Here's something wild: A photon takes about 8 minutes to travel from the Sun to Earth, but it can take 100,000 years to travel from the Sun's core to its surface.",
        "Did you know? The longest wedding veil was longer than 63 football fields.",
        "Fun fact: Crows can remember human faces and hold grudges.",
        "Here's a mind-bender: There are more ways to arrange a deck of 52 cards than there are atoms on Earth.",
        "Did you know? The average person walks about 100,000 miles in a lifetime. That's like walking around the Earth four times.",
        "Fun fact: An octopus has nine brains. One central brain and one in each arm.",
        "Here's something surprising: Hot water freezes faster than cold water. It's called the Mpemba effect.",
        "Did you know? The Great Wall of China is not actually visible from space with the naked eye.",
        "Fun fact: Sloths can hold their breath longer than dolphins, up to 40 minutes.",
        "Here's a wild fact: A single strand of spider silk is stronger than the same width of steel.",
        "Did you know? The smell of freshly cut grass is actually a plant distress signal.",
        "Fun fact: Your nose can remember 50,000 different scents.",
        "Here's something amazing: Elephants are the only animals that can't jump.",
        "Did you know? A group of porcupines is called a prickle.",
        "Fun fact: The average person spends about 6 months of their lifetime waiting for red lights to turn green.",
        "Here's a fun one: The unicorn is Scotland's national animal because in Celtic mythology it represented purity and power.",
        "Did you know? Humans share about 60 percent of their DNA with bananas.",
        "Fun fact: The dot over the letter i is called a tittle.",
        "Here's something interesting: A flea can jump up to 150 times its own body length.",
    )

    private var lastIndex = -1

    operator fun invoke(): String {
        var index: Int
        do {
            index = items.indices.random()
        } while (index == lastIndex && items.size > 1)
        lastIndex = index
        return items[index]
    }
}
