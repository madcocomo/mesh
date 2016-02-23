package com.gentics.mesh.core.verticle.microschema;

import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.UPDATE_PERM;
import static com.gentics.mesh.core.rest.common.GenericMessageResponse.message;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.gentics.mesh.core.data.MicroschemaContainer;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.data.schema.handler.MicroschemaComparator;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaModel;
import com.gentics.mesh.core.rest.schema.Microschema;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangesListModel;
import com.gentics.mesh.core.verticle.handler.AbstractCrudHandler;
import com.gentics.mesh.handler.InternalActionContext;
import com.gentics.mesh.json.JsonUtil;

import rx.Observable;

@Component
public class MicroschemaCrudHandler extends AbstractCrudHandler<MicroschemaContainer, Microschema> {

	@Autowired
	private MicroschemaComparator comparator;

	@Override
	public RootVertex<MicroschemaContainer> getRootVertex(InternalActionContext ac) {
		return boot.microschemaContainerRoot();
	}

	@Override
	public void handleUpdate(InternalActionContext ac) {

		db.asyncNoTrxExperimental(() -> {
			RootVertex<MicroschemaContainer> root = getRootVertex(ac);
			return root.loadObject(ac, "uuid", UPDATE_PERM).flatMap(element -> {
				try {
					Microschema requestModel = JsonUtil.readSchema(ac.getBodyAsString(), MicroschemaModel.class);
					SchemaChangesListModel model = new SchemaChangesListModel();
					model.getChanges().addAll(MicroschemaComparator.getIntance().diff(element.getSchema(), requestModel));
					if (model.getChanges().isEmpty()) {
						return Observable.just(message(ac, "schema_update_no_difference_detected", element.getName()));
					} else {
						return element.applyChanges(ac, model).flatMap(e -> {
							return Observable.just(message(ac, "migration_invoked", element.getName()));
						});
					}
				} catch (Exception e) {
					return Observable.error(e);
				}
			});
		}).subscribe(model -> ac.respond(model, OK), ac::fail);

	}

	@Override
	public void handleDelete(InternalActionContext ac) {
		deleteElement(ac, () -> getRootVertex(ac), "uuid", "microschema_deleted");
	}

	public void handleDiff(InternalActionContext ac) {
		db.asyncNoTrxExperimental(() -> {
			Observable<MicroschemaContainer> obsSchema = getRootVertex(ac).loadObject(ac, "uuid", READ_PERM);
			Microschema requestModel = JsonUtil.readSchema(ac.getBodyAsString(), MicroschemaModel.class);
			return obsSchema.flatMap(microschema -> microschema.diff(ac, comparator, requestModel));
		}).subscribe(model -> ac.respond(model, OK), ac::fail);
	}

	public void handleGetSchemaChanges(InternalActionContext ac) {
		// TODO Auto-generated method stub

	}

	public void handleApplySchemaChanges(InternalActionContext ac) {
		db.asyncNoTrxExperimental(() -> {
			Observable<MicroschemaContainer> obsSchema = boot.microschemaContainerRoot().loadObject(ac, "schemaUuid", UPDATE_PERM);
			return obsSchema.flatMap(schema -> {
				return schema.getLatestVersion().applyChanges(ac);
			});
		}).subscribe(model -> ac.respond(model, OK), ac::fail);

	}

}
