$(function() {
	$("#submit_button").click(function(event) {
		event.preventDefault();
		reqData = $("#input_request").val();
		$("#answer_content").text("Requesting answer for: " + reqData);
		$.ajax({
			type : "POST",
			url : "XMLCallback",
			contentType : "text/xml",
			processData : false,
			timout : 30 * 1000,
			data : reqData
		}).success(function(data) {
			var res = $(data).find("result") 
			if (res.length)
				$("#answer_content").text(res.text());
			else
				$("#answer_content").text(new XMLSerializer().serializeToString(data));
		}).fail(function(error, bla, text) {
			alert("ERROR!\n" + text);
			$("#answer.pre").html(error.responseText);
		});
	});
});