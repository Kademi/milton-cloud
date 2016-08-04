<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="x" uri="http://java.sun.com/jsp/jstl/xml" %>
<!DOCTYPE html>
<html lang="en">
    <head>

    </head>

    <body>
        <h1>Directory: ${model.directory.name}/</h1>
        <ul>
            <li><a href="../">..</a></li>
            <c:forEach items="${model.page.children}" var="node">
            <li>
                <a href="${node.href}" class="thumbnail">${node.name}</a>
            </li>
            </c:forEach>
        </ul>
    </body>
</html>

