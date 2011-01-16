package cs224n.langmodel;

import cs224n.util.Counter;
import cs224n.util.CounterMap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class AddDeltaLinearInterpolationBigram implements LanguageModel {
	private static final String START = "<S>";
	private static final String STOP = "</S>";
	private static final double BIGRAM_DELTA = 0.001;

	private CounterMap<String, String> bigramCounter;
	private Counter<String> wordCounter;
	private double totalWords, totalBigrams, vocabulary;

	// -----------------------------------------------------------------------

	/**
	 * Constructs a new, empty unigram language model.
	 */
	public AddDeltaLinearInterpolationBigram() {
		wordCounter = new Counter<String>();
		bigramCounter = new CounterMap<String, String>();
		totalWords = Double.NaN;
		vocabulary = Double.NaN;
	}

	/**
	 * Constructs a unigram language model from a collection of sentences. A
	 * special stop token is appended to each sentence, and then the frequencies
	 * of all words (including the stop token) over the whole collection of
	 * sentences are compiled.
	 */
	public AddDeltaLinearInterpolationBigram(Collection<List<String>> sentences) {
		this();
		train(sentences);
	}

	// -----------------------------------------------------------------------

	/**
	 * Constructs a unigram language model from a collection of sentences. A
	 * special stop token is appended to each sentence, and then the frequencies
	 * of all words (including the stop token) over the whole collection of
	 * sentences are compiled.
	 */
	public void train(Collection<List<String>> sentences) {
		wordCounter = new Counter<String>();
		bigramCounter = new CounterMap<String, String>();

		for (List<String> sentence : sentences) {
			List<String> stoppedSentence = new ArrayList<String>(sentence);
			stoppedSentence.add(STOP);
			wordCounter.incrementCount(START, 1.0);
			
			String prevWord = START;
			for (String word : stoppedSentence) {
				wordCounter.incrementCount(word, 1.0);
				bigramCounter.incrementCount(prevWord, word, 1.0);
				prevWord = word;
			}
		}

		totalWords = wordCounter.totalCount();
		vocabulary = wordCounter.size() + 1;
		totalBigrams = bigramCounter.totalCount();
	}

	// -----------------------------------------------------------------------

	private double getWordProbability(String word, String prevWord) {

		double jointCount = bigramCounter.getCount(prevWord, word);		// w1w2
		double prevCount = wordCounter.getCount(prevWord);				// w1
		double thisWordCount = wordCounter.getCount(word);				// w2

		/*
		 * if (jointCount == 0 || prevCount == 0) { // unknown word //
		 * System.out.println("UNKNOWN WORD: " + sentence.get(index)); return 0;
		 * //return 1.0 / (total + 1.0); }
		 */
		// if (jointCount == 0) // never-seen bigram
		// jointCount = 1;
		if (thisWordCount == 0) // unknown word
			thisWordCount = 1;

		return 0.7 * (jointCount + BIGRAM_DELTA) / (prevCount + vocabulary * BIGRAM_DELTA) + 0.3 * (thisWordCount / (totalWords + 1));
	}

	/**
	 * Returns the probability, according to the model, of the word specified by
	 * the argument sentence and index. Smoothing is used, so that all words get
	 * positive probability, even if they have not been seen before.
	 */
	public double getWordProbability(List<String> sentence, int index) {
		String word = sentence.get(index);
		String prevWord;
		if (index == 0)
			prevWord = START;
		else
			prevWord = sentence.get(index - 1);
		return getWordProbability(word, prevWord);
	}

	/**
	 * Returns the probability, according to the model, of the specified
	 * sentence. This is the product of the probabilities of each word in the
	 * sentence (including a final stop token).
	 */
	public double getSentenceProbability(List<String> sentence) {
		List<String> stoppedSentence = new ArrayList<String>(sentence);
		stoppedSentence.add(STOP);
		double probability = 1.0;
		for (int index = 0; index < stoppedSentence.size(); index++) {
			probability *= getWordProbability(stoppedSentence, index);
		}
		return probability;
	}

	/**
	 * checks if the probability distribution properly sums up to 1
	 */
	public double checkModel() {
		int randomIndex = (int)(Math.random() * (double)(wordCounter.keySet().size()));
		int count = 0;
		for (String preWord: wordCounter.keySet()) {
			if (count == randomIndex) {
				double sum = 0.0;
				for (String word : bigramCounter.getCounter(preWord).keySet()) {
					sum += getWordProbability(word, preWord);
				}
				return sum;
			}
			count++;
		}
		
		return 0.0;
		/*
		 * double sum = 0.0; // since this is a unigram model, // the event
		 * space is everything in the vocabulary (including STOP) // and a UNK
		 * token
		 * 
		 * // this loop goes through the vocabulary (which includes STOP) for
		 * (String word : wordCounter.keySet()) { sum +=
		 * getWordProbability(word); }
		 * 
		 * // remember to add the UNK. In this EmpiricalUnigramLanguageModel //
		 * we assume there is only one UNK, so we add... sum += 1.0 / (total +
		 * 1.0);
		 * 
		 * return sum;
		 */
		/*double sum = 0.0;
		for (String preWord : wordCounter.keySet()) {
			for (String word : wordCounter.keySet()) {
				sum += getWordProbability(word, preWord);
			}
		}
		
		return sum;*/
	}

	/**
	 * Returns a random word sampled according to the model. A simple
	 * "roulette-wheel" approach is used: first we generate a sample uniform on
	 * [0, 1]; then we step through the vocabulary eating up probability mass
	 * until we reach our sample.
	 */
	public String generateWord(String prevWord) {
		/*
		 * double sample = Math.random(); double sum = 0.0; for (String word :
		 * wordCounter.keySet()) { sum += wordCounter.getCount(word) /
		 * (total+1); if (sum > sample) { return word; } }
		 */
		return STOP;
		// return "*UNKNOWN*"; // a little probability mass was reserved for
		// unknowns
	}

	/**
	 * Returns a random sentence sampled according to the model. We generate
	 * words until the stop token is generated, and return the concatenation.
	 */
	public List<String> generateSentence() {
		List<String> sentence = new ArrayList<String>();
		String word = generateWord(START);
		while (!word.equals(STOP)) {
			sentence.add(word);
			word = generateWord(word);
		}
		return sentence;
	}

}
