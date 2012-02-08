$(function() {
	$("#p-broker").progressbar({
		value : 0
	});
	$("#p-reciever").progressbar({
		value : 0
	});

	$("#submit_button").click(function(event) {
		event.preventDefault();
		dataS = $("#form_data").serialize();
		$.get("SpeedStatistics/start_statistics?" + dataS).success(function(data) {
			$("#p-status").text("started");
			function worker() {
				$.get("SpeedStatistics/get_statistics").success(function(data) {

					st = $("sending-time", data);
					pt = $("processing-time", data);
					se = $("send-events", data);
					re = $("recieved-events", data);
					ee = $("exspected-events", data);

					eventno = parseInt(ee.text());
					sprogress = parseInt(se.text()) * 100.0 / eventno;
					rprogress = parseInt(re.text()) * 100.0 / eventno;

					$(".max-event-count").each(function() {
						$(this).text(eventno)
					});
					$("#event-send-count").text(se.text());
					$("#event-recieved-count").text(re.text());

					$("#p-broker").progressbar("option", "value", sprogress);
					$("#p-reciever").progressbar("option", "value", rprogress);

					if (pt.length > 0) {
						$("#event-send-value").text(se.text());
						$("#send-time-value").text(pt.text())
					}

					if (st.length > 0) {
						$("#event-recieved-value").text(re.text());
						$("#recieve-time-value").text(st.text());
						$("#p-status").text("finished");

						$("#delay-graphic").html("<embed src='SpeedStatistics/statistics.svg' width='430' height='270' type='image/svg+xml' pluginspage='http://www.adobe.com/svg/viewer/install/' />");
						$("#offset-graphic").html("<embed src='SpeedStatistics/out_of_order.svg' width='430' height='270' type='image/svg+xml' pluginspage='http://www.adobe.com/svg/viewer/install/' />");

					} else {

						setTimeout(worker, 2000);
					}
				});
			}
			;
			setTimeout(worker, 2000);

		}).fail(function(error, bla, text) {
			alert("ERROR!\n" + text);
			$("#answer.pre").html(error.responseText);
		});
	});
});