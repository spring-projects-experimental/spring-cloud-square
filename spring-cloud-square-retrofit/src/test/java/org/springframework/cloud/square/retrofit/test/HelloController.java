package org.springframework.cloud.square.retrofit.test;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

/**
 * @author Spencer Gibb
 */
@RestController
public abstract class HelloController implements TestConstants {

	@RequestMapping(method = GET, path = "/hello")
	public Hello getHello() {
		return new Hello(HELLO_WORLD_1);
	}

	@RequestMapping(method = POST, path = "/hello")
	public Hello postHello(@RequestBody Hello hello) {
		return new Hello(hello.getMessage());
	}

	@RequestMapping(method = GET, path = "/hellos")
	public List<Hello> getHellos() {
		ArrayList<Hello> hellos = getHelloList();
		return hellos;
	}

	@RequestMapping(method = GET, path = "/hellostrings")
	public List<String> getHelloStrings() {
		ArrayList<String> hellos = new ArrayList<>();
		hellos.add(HELLO_WORLD_1);
		hellos.add(OI_TERRA_2);
		return hellos;
	}

	@RequestMapping(method = GET, path = "/helloparams")
	public List<String> getParams(@RequestParam("params") List<String> params) {
		return params;
	}

	@RequestMapping(method = GET, path = "/noContent")
	ResponseEntity<Void> noContent() {
		return ResponseEntity.noContent().build();
	}

	@RequestMapping(method = RequestMethod.HEAD, path = "/head")
	ResponseEntity<Void> head() {
		return ResponseEntity.ok().build();
	}

	@RequestMapping(method = GET, path = "/fail")
	String fail() {
		throw new RuntimeException("always fails");
	}

	@RequestMapping(method = GET, path = "/notFound")
	ResponseEntity<String> notFound() {
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body((String) null);
	}

	public static ArrayList<Hello> getHelloList() {
		ArrayList<Hello> hellos = new ArrayList<>();
		hellos.add(new Hello(HELLO_WORLD_1));
		hellos.add(new Hello(OI_TERRA_2));
		return hellos;
	}

}
