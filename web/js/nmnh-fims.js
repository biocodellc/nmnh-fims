/* ====== General Utility Functions ======= */
var appRoot = "/fims/";
var sessionTimeout;

(function(undefined) {
    $(document).ready(function() {
        $(document).ajaxStop(function() {
            // session was refreshed, so cancel the existing inactivateSession and recall sessionCountdown();
            clearTimeout(sessionTimeout);
            sessionCountdown();
        });

        sessionCountdown();
    });
}).call(this);

function sessionCountdown() {
    // only invoke inactivateSession if the user is logged in
    if ($("#logout").length) {
        //  convert seconds to milliseconds
        sessionTimeout = setTimeout(inactivateSession, sessionMaxInactiveInterval * 1000);
    }
}

function inactivateSession() {
    var buttons = {
        "OK": function() {
            window.location.href = appRoot;
        }
    }
    var title = "Login Expired";
    var msg = "You're login session has expired. Please re-login.";
    dialog(msg, title, buttons, {closeOnEscape: false});
}

// function for displaying a loading dialog while waiting for a response from the server
function loadingDialog(promise) {
    var dialogContainer = $("#dialogContainer");
    var msg = "Loading ...";
    dialog(msg, "", null);

    // close the dialog when the ajax call has returned only if the html is the same
    promise.always(function(){
        if (dialogContainer.html() == msg) {
            dialogContainer.dialog("close");
        }
    });
}

function getQueryParam(sParam) {
    var sPageURL = window.location.search.substring(1);
    var sURLVariables = sPageURL.split('&');
    for (var i = 0; i < sURLVariables.length; i++) {
        var sParameterName = sURLVariables[i].split('=');
        if (sParameterName[0] == sParam) {
            if (sParam == "return_to") {
                // if we want the return_to query param, we need to return everything after "return_to="
                // this is assuming that "return_to" is the last query param, which it should be
                return decodeURIComponent(sPageURL.slice(sPageURL.indexOf(sParameterName[1])));
            } else {
                return decodeURIComponent(sParameterName[1]);
            }
        }
    }
}

function populateProjects() {
    theUrl = appRoot + "rest/projects/list";
    var jqxhr = $.getJSON( theUrl, function(data) {
        var listItems = "";
        listItems+= "<option value='0'>Select a department ...</option>";
        $.each(data, function(index, project) {
            listItems+= "<option value='" + project.projectId + "'>" + project.projectTitle + "</option>";
        });
        $("#projects").html(listItems);
        // Set to the first value in the list which should be "select one..."
        $("#projects").val($("#projects option:first").val());
    }).fail(function(jqXHR,textStatus) {
        if (textStatus == "timeout") {
	        showMessage ("Timed out, waiting for response! Try again later or reduce the number of graphs you are querying. If the problem persists, contact the System Administrator.");
        } else {
	        showMessage ("Error fetching projects!");
        }
    });
    return jqxhr;
}

function failError(jqxhr) {
    var buttons = {
        "OK": function(){
            $("#dialogContainer").removeClass("error");
            $(this).dialog("close");
          }
    }
    $("#dialogContainer").addClass("error");

    var message;
    if (jqxhr.responseJSON) {
        message = jqxhr.responseJSON.usrMessage;
    } else {
        message = "Server Error!";
    }
    dialog(message, "Error", buttons);
}

// function to open an new or update an already open jquery ui dialog box
function dialog(msg, title, buttons, opts) {
    var dialogContainer = $("#dialogContainer");
    if (dialogContainer.html() != msg) {
        dialogContainer.html(msg);
    }

    if (!$(".ui-dialog").is(":visible") || (dialogContainer.dialog("option", "title") != title ||
        dialogContainer.dialog("option" , "buttons") != buttons)) {
        dialogContainer.dialog($.extend({
            modal: true,
            autoOpen: true,
            title: title,
            resizable: false,
            width: 'auto',
            draggable: false,
            buttons: buttons,
            position: { my: "center top", at: "top", of: window}
        }, opts));
    }

    return;
}

//**
// A short message
function showMessage(message) {
$('#alerts').append(
        '<div class="alert">' +
            '<button type="button" class="close" data-dismiss="alert">' +
            '&times;</button>' + message + '</div>');
}

// Get the projectID
function getProjectID() {
    var e = document.getElementById('projects');
    return  e.options[e.selectedIndex].value;
}

/* ====== login.jsp Functions ======= */

// function to login user
function login() {
    var url = appRoot + "rest/authenticationService/loginLDAP";
    var return_to = getQueryParam("return_to");
    if (return_to != null) {
        url += "?return_to=" + return_to;
    }
    var jqxhr = $.post(url, $('form').serialize())
        .done(function(data) {
            window.location.replace(data.url);
        }).fail(function(jqxhr) {
            if (jqxhr.status == 404) {
                $(".error").html("page not found");
            } else if  (jqxhr.status == 302) {
                // a status of 302 must be returned in order to pass SI vulnerabilities assessment. This will fail the ajax call
                // however, it didn't really fail and the url will be returned in the json
                window.location.replace($.parseJSON(jqxhr.responseText).url);
            } else {
                $(".error").html($.parseJSON(jqxhr.responseText).usrMessage);
            }
        });
    loadingDialog(jqxhr);
}

// function to respond to entrust challenge questions
function challengeResponse() {
    var url = appRoot + "rest/authenticationService/entrustChallenge";
    var return_to = getQueryParam("return_to");
    if (return_to != null) {
        url += "?return_to=" + return_to;
    }
    var jqxhr = $.post(url, $('form').serialize())
        .done(function(data) {
            window.location.replace(data.url);
        }).fail(function(jqxhr) {
            // a status of 302 must be returned in order to pass SI vulnerabilities assessment. This will fail the ajax call
            // however, it didn't really fail and the url will be returned in the json
            if (jqxhr.status == 302) {
                window.location.replace($.parseJSON(jqxhr.responseText).url);
            } else {
                $(".error").html($.parseJSON(jqxhr.responseText).usrMessage);
            }
        });
    loadingDialog(jqxhr);
}

/* ====== templates.jsp Functions ======= */

// Processing functions
$(function () {
    $('input').click(populate_bottom);

    $('#default_bold').click(function() {
        $('.check_boxes').prop('checked',true);
        populate_bottom();
    });
    $('#excel_button').click(function() {
        var li_list = new Array();
        $(".check_boxes").each(function() {
            li_list.push($(this).text() );
        });
        if(li_list.length > 0){
            download_file();
        }
        else{
            showMessage('You must select at least 1 field in order to export a spreadsheet.');
        }
    });
})

// for template generator, get the definitions when the user clicks on DEF
function populateDefinitions(column) {
 var e = document.getElementById('projects');
    var projectId = e.options[e.selectedIndex].value;

    theUrl = appRoot + "rest/projects/" + projectId + "/getDefinition/" + column;

    $.ajax({
        type: "GET",
        url: theUrl,
        dataType: "html",
        success: function(data) {
            $("#definition").html(data);
        }
    });
}

function populateColumns(targetDivId) {
    var e = document.getElementById('projects');
    var projectId = e.options[e.selectedIndex].value;

    if (projectId != 0) {
        theUrl = appRoot + "rest/projects/" + projectId + "/attributes/";

        var jqxhr = $.ajax( {
            url: theUrl,
            async: false,
            dataType : 'html'
        }).done(function(data) {
            $(targetDivId).html(data);
        }).fail(function(jqXHR,textStatus) {
            if (textStatus == "timeout") {
                showMessage ("Timed out waiting for response!");
            } else {
                showMessage ("Error completing request!" );
            }
        });
        loadingDialog(jqxhr);

        $(".def_link").click(function () {
            populateDefinitions($(this).attr('name'));
        });
     }
}

function populateAbstract(targetDivId) {
    var e = document.getElementById('projects');
    var projectId = e.options[e.selectedIndex].value;

    theUrl = appRoot + "rest/projects/" + projectId + "/abstract/";

    var jqxhr = $.getJSON(theUrl
    ).done(function(data) {
        $(targetDivId).html(data.abstract +"<p>");
    }).fail(function(jqXHR,textStatus) {
        if (textStatus == "timeout") {
                showMessage ("Timed out waiting for response!");
        } else {
                showMessage ("Error completing request!" );
        }
    });
    loadingDialog(jqxhr);
}

var savedConfig;
function saveTemplateConfig() {
    var message = "<table><tr><td>Configuration Name:</td><td><input type='text' name='configName' /></td></tr></table>";
    var title = "Save Template Generator Configuration";
    var buttons = {
        "Save": function() {
            var checked = [];
            var configName = $("input[name='configName']").val();

            if (configName.toUpperCase() == "Default".toUpperCase()) {
                $("#dialogContainer").addClass("error");
                dialog("Talk to the project admin to change the default configuration.<br><br>" + message, title, buttons);
                return;
            }

            $("#cat1 input[type='checkbox']:checked").each(function() {
                checked.push($(this).data().uri);
            });

            savedConfig = configName;
            $.post(appRoot + "rest/projects/" + $("#projects").val() + "/saveTemplateConfig", $.param(
                                                            {"configName": configName,
                                                            "checkedOptions": checked,
                                                            "projectId": $("#projects").val()
                                                            }, true)
            ).done(function(data) {
                if (data.error != null) {
                    $("#dialogContainer").addClass("error");
                    var m = data.error + "<br><br>" + message;
                    dialog(m, title, buttons);
                } else {
                    $("#dialogContainer").removeClass("error");
                    populateConfigs();
                    var b = {
                        "Ok": function() {
                            $(this).dialog("close");
                        }
                    }
                    dialog(data.success + "<br><br>", "Success!", b);
                }
            }).fail(function(jqXHR) {
                failError(jqXHR);
            });
        },
        "Cancel": function() {
            $("#dialogContainer").removeClass("error");
            $(this).dialog("close");
        }
    }

    dialog(message, title, buttons);
}

function populateConfigs() {
    var projectId = $("#projects").val();
    if (projectId == 0) {
        $("#configs").html("<option value=0>Select a Project</option>");
    } else {
        var el = $("#configs");
        el.empty();
        el.append($("<option></option>").attr("value", 0).text("Loading configs..."));
        $.getJSON(appRoot + "rest/projects/" + projectId + "/getTemplateConfigs").done(function(data) {
            var listItems = "";

            el.empty();
            data.forEach(function(configName) {
                el.append($("<option></option>").
                    attr("value", configName).text(configName));
            });

            if (savedConfig != null) {
                $("#configs").val(savedConfig);
            }

            // if there are more then the default config, show the remove link
            if (data.length > 1) {
                if ($('.toggle-content#remove_config_toggle').is(':hidden')) {
                    $('.toggle-content#remove_config_toggle').show(400);
                }
            } else {
                if (!$('.toggle-content#remove_config_toggle').is(':hidden')) {
                    $('.toggle-content#remove_config_toggle').hide();
                }
            }

        }).fail(function(jqXHR,textStatus) {
            if (textStatus == "timeout") {
                showMessage ("Timed out waiting for response! Try again later or reduce the number of graphs you are querying. If the problem persists, contact the System Administrator.");
            } else {
                showMessage ("Error fetching template configurations!");
            }
        });
    }
}

function updateCheckedBoxes() {
    var configName = $("#configs").val();
    if (configName == "Default") {
        populateColumns("#cat1");
    } else {
        $.getJSON(appRoot + "rest/projects/" + $("#projects").val() + "/getTemplateConfig/" + configName.replace("/\//g", "%2F")).done(function(data) {
            if (data.error != null) {
                showMessage(data.error);
                return;
            }
            // deselect all unrequired columns
            $(':checkbox').not(":disabled").each(function() {
                this.checked = false;
            });

            data.checkedOptions.forEach(function(uri) {
                $(':checkbox[data-uri="' + uri + '"]')[0].checked = true;
            });
        }).fail(function(jqXHR, textStatus) {
            if (textStatus == "timeout") {
                showMessage ("Timed out waiting for response! Try again later or reduce the number of graphs you are querying. If the problem persists, contact the System Administrator.");
            } else {
                showMessage ("Error fetching template configuration!");
            }
        });
    }
}

function removeConfig() {
    var configName = $("#configs").val();
    if (configName == "Default") {
        var buttons = {
            "Ok": function() {
                $(this).dialog("close");
            }
        }
        dialog("You can not remove the Default configuration", title, buttons);
        return;
    }

    var message = "Are you sure you want to remove "+ configName + " configuration?";
    var title = "Warning";
    var buttons = {
        "OK": function() {
            var buttons = {
                "Ok": function() {
                    $(this).dialog("close");
                }
            }
            var title = "Remove Template Generator Configuration";

            $.getJSON(appRoot + "rest/projects/" + $("#projects").val() + "/removeTemplateConfig/" + configName.replace("/\//g", "%2F")).done(function(data) {
                if (data.error != null) {
                    showMessage(data.error);
                    return;
                }

                populateConfigs();
                dialog(data.success, title, buttons);
            }).fail(function(jqXHR) {
                failError(jqXHR);
            });
        },
        "Cancel": function() {
            $("#dialogContainer").removeClass("error");
            $(this).dialog("close");
        }
    }

    dialog(message, title, buttons);
}

function populate_bottom(){
    var selected = new Array();
    var listElement = document.createElement("ul");
    listElement.className = 'picked_tags';
    $("#checked_list").html(listElement);
    $("input:checked").each(function() {
        var listItem = document.createElement("li");
        listItem.className = 'picked_tags_li';
        listItem.innerHTML = ($(this).val());
        listElement.appendChild(listItem);
    });
}

function download_file(){
    NMNHDialog().then(function(accessionNumber, datasetCode, operation) {
        var url = appRoot + 'rest/projects/createExcel/';
        var input_string = '';
        // Loop through CheckBoxes and find ones that are checked
        $(".check_boxes").each(function(index) {
            if ($(this).is(':checked'))
                input_string+='<input type="hidden" name="fields" value="' + $(this).val() + '" />';
        });
        input_string+='<input type="hidden" name="projectId" value="' + getProjectID() + '" />';

        if (accessionNumber != null) {
            input_string += '<input type="hidden" name="accession_number" value="' + accessionNumber + '" />' +
                '<input type="hidden" name="dataset_code" value="' + datasetCode + '" />';
        }

        // is this an update or insert operation (no need to check this again on server)
        input_string += '<input type="hidden" name="operation" value="' + operation +'" />';

        // Pass the form to the server and submit
        $('<form action="'+ url +'" method="post">'+input_string+'</form>').appendTo('body').submit().remove();
    });
}

// show a dialog to get the user's accession number and unique collection number
function NMNHDialog() {

    var d = new $.Deferred();
    var title = "NMNH Project Additional Information"
    var message = "This is a NMNH project. Please enter:<br>" +
        "Accession Number: <input type='text' id='accession_number' /><br>" +
        "Dataset Code: <input type='text' id='dataset_code' />";

    var buttons = {
        "Create": function() {
            var aRE = /^\d{6,20}$/;
            var dRE = /^[a-zA-Z0-9_]{8,50}$/

            if (!aRE.test($("#accession_number").val()) || !dRE.test($("#dataset_code").val())) {
                var error = "<br><p class=error>" +
                "<b>Accession</b> must be an integer with greater or equal to 6 numbers"+
                    "<br><b>Dataset Code</b> must contain only numbers, letters, or underscores and be 8 to 50 characters long</p>";
                dialog(message + error, title, buttons);
            // Call
            } else {

                // Save the context of this so it can be used inside the POST
                var $this = $(this);

                $.getJSON(appRoot + "rest/expeditions/validate/" + $("#projects").val() + "/" + encodeURIComponent($("#dataset_code").val()))
                    .done(function(data) {
                        if (data.update) {
                            var buttons = {
                                "Continue": function() {
                                    d.resolve($("#accession_number").val(), $("#dataset_code").val(),"update");
                                    $(this).dialog("close");
                                },
                                "Cancel": function() {
                                    d.reject();
                                    $(this).dialog("close");
                                }
                            }
                            // remember accession_number, dataset_code values using hidden form elements
                            dialog("Warning: Dataset Code '" + $("#dataset_code").val() + "' already exists." +
                             "<br>The Dataset Code designates a globally unique key for this dataset. " +
                            "<br>Select CONTINUE only if you wish to update column names for a spreadsheet you recently downloaded, otherwise " +
                            "<br>select CANCEL to type in a new Dataset Code" +
                            "<input type=hidden id='accession_number' value='"+$("#accession_number").val()+"' />" +
                            "<input type=hidden id='dataset_code' value='"+$("#dataset_code").val()+"' />"
                            , "Dataset Code", buttons);
                        } else {
                            d.resolve($("#accession_number").val(), $("#dataset_code").val(), "insert");
                            $this.dialog("close");
                        }
                    }).fail(function(jqxhr) {
                        var cancelbutton = { "Cancel": function() {
                                d.reject();
                                $(this).dialog("close");
                            }
                        }
                        // Process status codes from server
                        if (jqxhr.status == 401) {
                            var message = "Server message<br><br>" + JSON.stringify($.parseJSON(jqxhr.responseText).error);
                            dialog(message, "Dataset Error", cancelbutton);
                        } else if (jqxhr.status != 404) {
                            var message = "Server responded with HTTP status code = "+ jqxhr.status;
                            dialog(message, "Dataset Error", cancelbutton);
                        } else {
                            var message = "Dataset validation failed.<br><br>" + JSON.stringify($.parseJSON(jqxhr.responseText).error);
                            dialog(message, "Dataset Error", cancelbutton);
                        }
                    });
            }
        },
        "Cancel": function() {
            d.reject();
            $(this).dialog("close");
        }
    }
    dialog(message, title, buttons);
    return d.promise();
}

/* ====== validation.jsp Functions ======= */

function parseSpreadsheet(regExpression, sheetName) {
    try {
        f = new FileReader();
    } catch(err) {
        return null;
    }
    var deferred = new $.Deferred();
    // older browsers don't have a FileReader
    if (f != null) {
        var inputFile= $('#dataset')[0].files[0];

        var splitFileName = $('#dataset').val().split('.');
        if ($.inArray(splitFileName[splitFileName.length - 1], XLSXReader.exts) > -1) {
            $.when(XLSXReader.utils.findCell(inputFile, regExpression, sheetName)).done(function(match) {
                if (match) {
                    deferred.resolve(match.toString().split('=')[1].slice(0, -1));
                } else {
                    deferred.resolve(null);
                }
            });
            return deferred.promise();
        }
    }
    setTimeout(function(){deferred.resolve(null)}, 100);
    return deferred.promise();

}

// Get the projectId for a key/value expression
function getProjectKeyValue() {
    return "projectId=" + getProjectID();
}

// Check that the validation form has a project id and if uploading, has an expedition code
function validForm() {
    if ($('#projects').val() == 0 || $("#upload").is(":checked")) {
        var message;
        var error = false;

        if ($("#dataset").val().length < 1) {
            message = "Please select a spreadsheet";
            error = true;
        } else if ($('#projects').val() == 0) {
            message = "Please select a project.";
            error = true;
        } else if ($("#upload").is(":checked") && ($('#expeditionCode').val() == null ||
            $('#expeditionCode').val().length < 8)) {
            message = "Dataset code is too short. Must be between 8 and 50 characters.";
            error = true;
        } else if ($("#upload").is(":checked") && ($('#expeditionCode').val().length > 50)) {
            message = "Dataset code is too long. Please limit to 50 characters.";
            error = true;
        }
        if (error) {
            $('#resultsContainer').html(message);
            var buttons = {
                "OK": function(){
                    $(this).dialog("close");
                  }
            }
            dialog(message, "Validation Results", buttons);
            return false;
        }
    }
    return true;
}

// submit dataset to be validated/uploaded
function validatorSubmit() {
    if (validForm()) {
        submitForm().done(function(data) {
            validationResults(data);
        }).fail(function(jqxhr) {
            failError(jqxhr);
        });
    }
}

// keep looping pollStatus every second until results are returned
function loopStatus(promise) {
    setTimeout( function() {
        pollStatus()
            .done(function(data) {
                if (promise.state() == "pending") {
                    if (data.error != null) {
                        dialog(data.error, "Validation Results");
                    } else {
                        dialog(data.status, "Validation Results");
                    }
                    loopStatus(promise);
                }
            });
    }, 1000);
}

// poll the server to get the validation/upload status
function pollStatus() {
    var def = new $.Deferred();
    $.getJSON(appRoot + "rest/validate/status")
        .done(function(data) {
            def.resolve(data);
        }).fail(function() {
            def.reject();
        });
    return def.promise();
}

// Continue the upload process after getting user consent if there were warnings during validation or if we are creating
// a new expedition
function continueUpload(createExpedition) {
    var d = new $.Deferred();
    var url = appRoot + "rest/validate/continue";
    if (createExpedition) {
        url += "?createExpedition=true";
    }
    $.getJSON(url)
        .done(function(data) {
            d.resolve();
            uploadResults(data);
        }).fail(function(jqxhr) {
            d.reject();
            failError(jqxhr);
        });
    loopStatus(d.promise());
}

// function to verify naan's
function checkNAAN(spreadsheetNaan, naan) {
    if (spreadsheetNaan != naan) {
        var buttons = {
            "Ok": function() {
            $("#dialogContainer").removeClass("error");
            $(this).dialog("close");
            }
        }
        var message = "Spreadsheet appears to have been created using a different FIMS system.<br>";
        message += "Spreadsheet says NAAN = " + spreadsheetNaan + "<br>";
        message += "System says NAAN = " + naan + "<br>";
        message += "Proceed only if you are SURE that this spreadsheet is being called.<br>";
        message += "Otherwise, re-load the proper FIMS system or re-generate your spreadsheet template.";

        dialog(message, "NAAN check", buttons);
    }
}

// function to toggle the project_id and expedition_code inputs of the validation form
function validationFormToggle() {
    $('#dataset').change(function() {

        // Check NAAN
        $.when(parseSpreadsheet("~naan=[0-9]+~", "Instructions")).done(function(spreadsheetNaan) {
            if (spreadsheetNaan > 0) {
                $.getJSON(appRoot + "rest/utils/getNAAN/")
                        .done(function(data) {
                    checkNAAN(spreadsheetNaan, data.naan);
                });
            }
        });

        $.when(parseSpreadsheet("~project_id=[0-9]+~", "Instructions")).done(function(project_id) {
            if (project_id > 0) {
                $('#projects').val(project_id);
                $('#projects').prop('disabled', true);
                $('#projects').trigger("change");
                if ($('.toggle-content#projects_toggle').is(':hidden')) {
                    $('.toggle-content#projects_toggle').show(400);
                }
            } else {
                $('#projects').prop('disabled', false);
                if (!$("#dataset").val()) {
                    $(".toggle-content#projects_toggle").hide(400);
                } else if ($('.toggle-content#projects_toggle').is(':hidden')) {
                    $('.toggle-content#projects_toggle').show(400);
                }
            }
        });
    });
    $('#upload').change(function() {
        if ($('.toggle-content#expeditionCode_toggle').is(':hidden') && $('#upload').is(":checked")) {
            $('.toggle-content#expeditionCode_toggle').show(400);
        } else {
            $('.toggle-content#expeditionCode_toggle').hide(400);
        }
    });
    $("#projects").change(function() {
        // only get expedition codes if a user is logged in
        if ($('*:contains("Logout")').length > 0) {
            $("#expeditionCode").replaceWith("<p id='expeditionCode'>Loading ... </p>");
            if(oldBrowser) {
                    getExpeditionCodes();
            } else {
                $.when(parseSpreadsheet("~dataset_code=[a-zA-Z0-9-_]{8,50}~", "Instructions")).done(function(dataset_code) {
                    if (dataset_code != null) {
                        //$("#expedition_code").replaceWith('<input type="hidden" name="expedition_code" id="expedition_code">' + dataset_code);
                        $("#expeditionCode_container").html('<input type="hidden" name="expeditionCode" id="expeditionCode">' + dataset_code);
                        $("#expeditionCode").val(dataset_code);
                    } else {
                        // getExpeditionCodes();
                        alert("Problem reading dataset code from spreadsheet. Is the dataset code embeded in the spreadsheet?");
                    }
                });
            }
        }
    });
}

// get the expeditions codes a user owns for a project
function getExpeditionCodes() {
    var projectID = $("#projects").val();
    $.getJSON(appRoot + "rest/projects/" + projectID + "/expeditions/")
        .done(function(data) {
            var select = "<select name='expeditionCode' id='expeditionCode' style='max-width:199px'>" +
                "<option value='0'>Create New Expedition</option>";
            $.each(data.expeditions, function(key, e) {
                select += "<option value=" + e.expeditionCode + ">" + e.expeditionCode + " (" + e.expeditionTitle + ")</option>";
            });

            select += "</select>";
            $("#expeditionCode").replaceWith(select);
        }).fail(function(jqxhr) {
            $("#expeditionCode").replaceWith('<input type="text" name="expeditionCode" id="expeditionCode" />');
            $("#dialogContainer").addClass("error");
            var buttons = {
                "Ok": function() {
                $("#dialogContainer").removeClass("error");
                $(this).dialog("close");
                }
            }
            dialog("Error fetching expeditions!<br><br>" + JSON.stringify($.parseJSON(jqxhr.responseText).usrMessage), "Error!", buttons)
        });
}

// function to handle the results from the rest service /fims/rest/validate
function validationResults(data) {
    var title = "Validation Results";
    if (data.done != null) {
        var buttons = {
            "Ok": function() {
                $(this).dialog("close");
            }
        }
        $("#dialogContainer").dialog("close");
        writeResults(data.done);
    } else {
        if (data.continue_message.message == null) {
            continueUpload(false);
        } else {
            // ask user if want to proceed
            var buttons = {
                "Continue": function() {
                      continueUpload(false);
                },
                "Cancel": function() {
                    writeResults(data.continue_message.message);
                    $(this).dialog("close");
                }
            }
            dialog(data.continue_message.message, title, buttons);
        }
    }
}

// function to handle the results from the rest service /fims/rest/validate/continue
function uploadResults(data) {
    var title = "Upload Results";
    if (data.done != null || data.error != null) {
        var message;
        if (data.done != null) {
            message = data.done;
            writeResults(message);
        } else {
            $("#dialogContainer").addClass("error");
            message = data.error;
        }
        var buttons = {
            "Ok": function() {
                $("#dialogContainer").removeClass("error");
                $(this).dialog("close");
            }
        }
        dialog(message, title, buttons);
        // reset the form to default state
        $('form').clearForm();
        $('.toggle-content#projects_toggle').hide(400);
        $('.toggle-content#expeditionCode_toggle').hide(400);

    } else {
        // ask user if want to proceed
        var buttons = {
            "Continue": function() {
                continueUpload(true);
            },
            "Cancel": function() {
                $(this).dialog("close");
            }
        }
        dialog(data.continue_message, title, buttons);
    }
}

// write results to the resultsContainer
function writeResults(message) {
    $("#resultsContainer").show();
    // Add some nice coloring
    message= message.replace(/Warning:/g,"<span style='color:orange;'>Warning:</span>");
    message= message.replace(/Error:/g,"<span style='color:red;'>Error:</span>");
    // set the project key for any project_id expressions... these come from the validator to call REST services w/ extra data
    message= message.replace(/project_id=/g,getProjectKeyValue());
    $("#resultsContainer").html("<table><tr><td>" + message + "</td></tr></table>");
}

// function to submit the validation form using jquery form plugin to handle the file uploads
function submitForm(){
    var de = new $.Deferred();
    var promise = de.promise();
    var options = {
        url: appRoot + "rest/validate/",
        type: "POST",
        contentType: "multipart/form-data",
        beforeSerialize: function(form, options) {
            $('#projects').prop('disabled', false);
        },
        beforeSubmit: function(form, options) {
            $('#projects').prop('disabled', true);
            dialog("Loading ...", "Validation Results", null);
            // For browsers that don't support the upload progress listener
            var xhr = $.ajaxSettings.xhr();
            if (!xhr.upload) {
                loopStatus(promise)
            }
        },
        error: function(jqxhr) {
            de.reject(jqxhr);
        },
        success: function(data) {
            de.resolve(data);
        },
        fail: function(jqxhr) {
            de.reject(jqxhr);
        },
        uploadProgress: function(event, position, total, percentComplete) {
            // For browsers that do support the upload progress listener
            if (percentComplete == 100) {
            loopStatus(promise)
            }
        }
    }

    $('form').ajaxSubmit(options);
    return promise;
}

/* ====== datasets.jsp Functions ======= */

// function to retrieve the user's datasets
function getDatasetDashboard() {
    theUrl = appRoot + "rest/utils/getDatasetDashboard";
    var jqxhr = $.getJSON( theUrl, function(data) {
        $("#dashboard").html(data.dashboard);
        // attach toggle function to each project
        $(".expand-content").click(function() {
            projectToggle(this.id)
        });
    }).fail(function() {
        $("#dashboard").html("Failed to load datasets from server.");
    });
}

// function to apply the jquery slideToggle effect.
function projectToggle(id) {
    // escape special characters in id field
    id = id.replace(/([!@#$%^&*()+=\[\]\\';,./{}|":<>?~_-])/g, "\\$1");
    // store the element value in a field
    var idElement = $('.toggle-content#'+id);
    if (idElement.is(':hidden')) {
        $('.img-arrow', '#'+id).attr("src", appRoot + "images/down-arrow.png");
    } else {
        $('.img-arrow', '#'+id).attr("src", appRoot + "images/right-arrow.png");
    }
    $(idElement).slideToggle('slow');
}
