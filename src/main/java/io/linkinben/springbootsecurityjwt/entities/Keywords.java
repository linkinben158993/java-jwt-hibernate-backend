package io.linkinben.springbootsecurityjwt.entities;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

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
