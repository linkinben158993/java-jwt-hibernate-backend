package io.linkinben.springbootsecurityjwt.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity(name = "keywords")
@Table(name = "keywords", uniqueConstraints = { @UniqueConstraint(columnNames = "word"),
		@UniqueConstraint(columnNames = "wId") })
public class Keywords {

	@Id
	@Column(name = "wId")
	private String wordId;

	@Column(name = "word")
	private String word;

	public String getWordId() {
		return wordId;
	}

	public void setWordId(String wordId) {
		this.wordId = wordId;
	}

	public String getWord() {
		return word;
	}

	public void setWord(String word) {
		this.word = word;
	}

}
