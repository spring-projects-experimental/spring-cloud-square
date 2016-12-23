package org.springframework.cloud.retrofit.test;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author Spencer Gibb
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Hello {
	private String message;
}
